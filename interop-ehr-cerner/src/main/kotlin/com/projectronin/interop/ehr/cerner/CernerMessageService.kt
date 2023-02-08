package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.MessageService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.cerner.exception.ResourceCreateException
import com.projectronin.interop.ehr.inputs.EHRMessageInput
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Communication
import com.projectronin.interop.fhir.r4.resource.CommunicationPayload
import com.projectronin.interop.fhir.r4.valueset.EventStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Cerner
import io.opentracing.util.GlobalTracer
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class CernerMessageService(cernerClient: CernerClient, private val patientService: CernerPatientService) :
    MessageService,
    CernerFHIRService<Communication>(cernerClient) {
    private val logger = KotlinLogging.logger { }

    override val fhirURLSearchPart = "/Communication"
    override val fhirResourceType = Communication::class.java
    override val writeScope = true

    override fun sendMessage(tenant: Tenant, messageInput: EHRMessageInput): String {
        val vendor = tenant.vendor
        if (vendor !is Cerner) {
            throw IllegalStateException("Tenant is not Cerner vendor: ${tenant.mnemonic}")
        }

        logger.info { "Send Message started for ${tenant.mnemonic}" }
        val communication = createCommunication(tenant, vendor, messageInput)
        val response = runBlocking { cernerClient.post(tenant, fhirURLSearchPart, communication) }

        val resourceLocation = response.headers["Location"]
            ?: throw ResourceCreateException(tenant, fhirURLSearchPart) { "No Location header received" }
        val createdFhirId = Reference.getId(resourceLocation)
            ?: throw ResourceCreateException(
                tenant,
                fhirURLSearchPart
            ) { "Returned location ($resourceLocation) is not a valid Reference" }

        // Add the response FHIR ID to our log spans to get better visibility when debugging in DataDog.
        val span = GlobalTracer.get().activeSpan()
        span?.let { it.setTag("message.responseId", createdFhirId) }

        return createdFhirId
    }

    private fun createCommunication(tenant: Tenant, cerner: Cerner, messageInput: EHRMessageInput): Communication {
        val patientFhirId = patientService.getPatientFHIRId(tenant, messageInput.patientMRN)
        val recipients = messageInput.recipients.map { Reference(reference = FHIRString("Practitioner/${it.id}")) }
        val sender = "Practitioner/${cerner.messagePractitioner}"
        val topic = cerner.messageTopic ?: "Ronin Symptoms Alert"
        val category = cerner.messageCategory ?: "notification"
        val priority = cerner.messagePriority ?: "urgent"
        val encodedText = Base64.getEncoder().encodeToString(messageInput.text.toByteArray())

        return Communication(
            status = EventStatus.COMPLETED.asCode(),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/communication-category"),
                            code = Code(category)
                        )
                    )
                )
            ),
            priority = Code(priority),
            subject = Reference(reference = FHIRString("Patient/$patientFhirId")),
            topic = CodeableConcept(text = FHIRString(topic)),
            recipient = recipients,
            sender = Reference(reference = FHIRString(sender)),
            payload = listOf(
                CommunicationPayload(
                    content = DynamicValue(
                        DynamicValueType.ATTACHMENT,
                        Attachment(
                            contentType = Code("text/plain"),
                            data = Base64Binary(encodedText)
                        )
                    )
                )
            )
        )
    }
}
