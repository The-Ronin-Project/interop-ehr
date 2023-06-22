package com.projectronin.interop.ehr.epic

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.MessageService
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRecipient
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageResponse
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.EHRMessageInput
import com.projectronin.interop.ehr.inputs.EHRRecipient
import com.projectronin.interop.ehr.inputs.IdentifierVendorIdentifier
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.tenant.config.ProviderPoolService
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import datadog.trace.api.Trace
import io.opentracing.util.GlobalTracer
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Service for facilitating sending messages to Epic.
 *
 * See: [SendMessage Documentation](https://apporchard.epic.com/Sandbox?api=384)
 */
@Component
class EpicMessageService(
    private val epicClient: EpicClient,
    private val providerPoolService: ProviderPoolService,
    private val ehrDataAuthorityClient: EHRDataAuthorityClient,
    private val identifierService: EpicIdentifierService
) :
    MessageService {
    private val logger = KotlinLogging.logger { }
    private val sendMessageUrlPart = "/api/epic/2014/Common/Utility/SENDMESSAGE/Message"

    @Trace
    override fun sendMessage(tenant: Tenant, messageInput: EHRMessageInput): String {
        val vendor = tenant.vendor
        if (vendor !is Epic) throw IllegalStateException("Tenant is not Epic vendor: ${tenant.mnemonic}")
        logger.info { "SendMessage started for ${tenant.mnemonic}" }

        val patient = runBlocking {
            ehrDataAuthorityClient.getResourceAs<Patient>(
                tenant.mnemonic,
                "Patient",
                messageInput.patientFHIRID.localize(tenant)
            ) ?: throw VendorIdentifierNotFoundException("No Patient found for ${messageInput.patientFHIRID}")
        }
        val mrn = identifierService.getMRNIdentifier(tenant, patient.identifier)
        val mrnValue =
            mrn.value?.value ?: throw VendorIdentifierNotFoundException("Failed to find a value on Patient's MRN")

        val sendMessageRequest =
            translateMessageInput(messageInput, vendor.ehrUserId, vendor.messageType, tenant, mrnValue)

        val span = GlobalTracer.get().activeSpan()
        span?.let {
            it.setTag(
                "message.recipients",
                sendMessageRequest.recipients?.joinToString { r -> "${if (r.isPool) "pool" else "user"}:${r.iD}" }
            )
        }

        val response = try {
            runBlocking {
                epicClient.post(tenant, sendMessageUrlPart, sendMessageRequest).body<SendMessageResponse>()
            }
        } catch (e: Exception) { // further investigation required to see if this is a sustainable solution
            logger.error { "SendMessage failed for ${tenant.mnemonic}, with exception ${e.message}" }
            throw e
        }
        logger.info { "SendMessage completed for ${tenant.mnemonic}" }

        val resultId = response.idTypes[0].id

        span?.let {
            it.setTag("message.responseId", "${response.idTypes[0].type}:$resultId")
        }

        // Return the id of the message
        return resultId
    }

    private fun translateMessageInput(
        messageInput: EHRMessageInput,
        userId: String,
        messageType: String,
        tenant: Tenant,
        patientMRN: String
    ): SendMessageRequest {
        return SendMessageRequest(
            patientID = patientMRN,
            patientIDType = (tenant.vendor as Epic).patientMRNTypeText,
            recipients = translateRecipients(messageInput.recipients, tenant),
            messageText = messageInput.text.split("\r\n", "\n").map { if (it.isEmpty()) " " else it },
            senderID = userId,
            messageType = messageType
        )
    }

    private fun translateRecipients(
        recipients: List<EHRRecipient>,
        tenant: Tenant
    ): List<SendMessageRecipient> {
        // The Epic implementation of sendMessage expects the VendorIdentifier to be an IdentifierVendorIdentifier.
        // with a value. If it isn't, this will rightfully throw an exception.
        val idList =
            recipients.map {
                val identifierVendorIdentifier = it.identifier as IdentifierVendorIdentifier
                identifierVendorIdentifier.identifier.value?.value ?: throw VendorIdentifierNotFoundException()
            }
        val poolList = providerPoolService.getPoolsForProviders(tenant, idList)

        return idList.map { userID ->
            val poolID = poolList[userID]
            SendMessageRecipient(
                iD = poolID ?: userID,
                isPool = (poolID != null)
            )
        }.toList().distinct() // deduplicate
    }
}
