package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.cerner.exception.ResourceCreateException
import com.projectronin.interop.ehr.inputs.EHRMessageInput
import com.projectronin.interop.ehr.inputs.EHRRecipient
import com.projectronin.interop.ehr.inputs.IdVendorIdentifier
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Communication
import com.projectronin.interop.fhir.r4.resource.CommunicationPayload
import com.projectronin.interop.fhir.r4.valueset.EventStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CernerMessageServiceTest {
    private val cernerClient = mockk<CernerClient>()
    private val messageService = CernerMessageService(cernerClient)

    @Test
    fun `write scope is enabled`() {
        assertTrue(messageService.writeScope)
    }

    @Test
    fun `throws exception when not a Cerner vendor`() {
        val tenant = mockk<Tenant> {
            every { mnemonic } returns "test"
            every { vendor } returns mockk<Epic>()
        }

        val messageInput = mockk<EHRMessageInput>()
        val exception = assertThrows<IllegalStateException> { messageService.sendMessage(tenant, messageInput) }
        assertEquals("Tenant is not Cerner vendor: test", exception.message)
    }

    @Test
    fun `throws exception when no Location header in response`() {
        val tenant = createTestTenant(
            mnemonic = "test",
            practitioner = "SendingPractitioner",
            messageTopic = "From Ronin",
            messageCategory = "alert",
            messagePriority = "routine"
        )
        val messageInput = EHRMessageInput(
            text = "This is a simple message.",
            "PatientFhirId",
            listOf(EHRRecipient("Recipient1", IdVendorIdentifier(Id("Recipient1"))))
        )

        val expectedCommunication = createCommunication(
            patientFhirId = "PatientFhirId",
            topic = "From Ronin",
            category = "alert",
            priority = "routine",
            sender = Reference(reference = "Practitioner/SendingPractitioner".asFHIR()),
            recipients = listOf(Reference(reference = "Practitioner/Recipient1".asFHIR())),
            encodedText = "VGhpcyBpcyBhIHNpbXBsZSBtZXNzYWdlLg=="
        )
        coEvery { cernerClient.post(tenant, "/Communication", expectedCommunication) } returns mockk {
            every { httpResponse.headers } returns mockk {
                every { this@mockk["Location"] } returns null
            }
        }

        val exception = assertThrows<ResourceCreateException> { messageService.sendMessage(tenant, messageInput) }
        assertEquals("Exception when calling /Communication for test: No Location header received", exception.message)
    }

    @Test
    fun `throws exception when invalid Location header in response`() {
        val tenant = createTestTenant(
            mnemonic = "test",
            practitioner = "SendingPractitioner",
            messageTopic = "From Ronin",
            messageCategory = "alert",
            messagePriority = "routine"
        )
        val messageInput = EHRMessageInput(
            text = "This is a simple message.",
            "PatientFhirId",
            listOf(EHRRecipient("Recipient1", IdVendorIdentifier(Id("Recipient1"))))
        )

        val expectedCommunication = createCommunication(
            patientFhirId = "PatientFhirId",
            topic = "From Ronin",
            category = "alert",
            priority = "routine",
            sender = Reference(reference = "Practitioner/SendingPractitioner".asFHIR()),
            recipients = listOf(Reference(reference = "Practitioner/Recipient1".asFHIR())),
            encodedText = "VGhpcyBpcyBhIHNpbXBsZSBtZXNzYWdlLg=="
        )
        coEvery { cernerClient.post(tenant, "/Communication", expectedCommunication) } returns mockk {
            every { httpResponse.headers } returns mockk {
                every { this@mockk["Location"] } returns "http://projectronin.com"
            }
        }

        val exception = assertThrows<ResourceCreateException> { messageService.sendMessage(tenant, messageInput) }
        assertEquals(
            "Exception when calling /Communication for test: Returned location (http://projectronin.com) is not a valid Reference",
            exception.message
        )
    }

    @Test
    fun `successful for single recipient`() {
        val tenant = createTestTenant(
            mnemonic = "test",
            practitioner = "SendingPractitioner",
            messageTopic = "From Ronin",
            messageCategory = "alert",
            messagePriority = "routine"
        )
        val messageInput = EHRMessageInput(
            text = "This is a simple message.",
            "PatientFhirId",
            listOf(EHRRecipient("Recipient1", IdVendorIdentifier(Id("Recipient1"))))
        )

        val expectedCommunication = createCommunication(
            patientFhirId = "PatientFhirId",
            topic = "From Ronin",
            category = "alert",
            priority = "routine",
            sender = Reference(reference = "Practitioner/SendingPractitioner".asFHIR()),
            recipients = listOf(Reference(reference = "Practitioner/Recipient1".asFHIR())),
            encodedText = "VGhpcyBpcyBhIHNpbXBsZSBtZXNzYWdlLg=="
        )
        coEvery { cernerClient.post(tenant, "/Communication", expectedCommunication) } returns mockk {
            every { httpResponse.headers } returns mockk {
                every { this@mockk["Location"] } returns "Communication/1-2-3-4"
            }
        }

        val response = messageService.sendMessage(tenant, messageInput)
        assertEquals("1-2-3-4", response)
    }

    @Test
    fun `successful for multiple recipients`() {
        val tenant = createTestTenant(
            mnemonic = "test",
            practitioner = "SendingPractitioner",
            messageTopic = "From Ronin",
            messageCategory = "alert",
            messagePriority = "routine"
        )
        val messageInput = EHRMessageInput(
            text = "This is a simple message.",
            "PatientFhirId",
            listOf(
                EHRRecipient("Recipient1", IdVendorIdentifier(Id("Recipient1"))),
                EHRRecipient("Recipient2", IdVendorIdentifier(Id("Recipient2")))
            )
        )

        val expectedCommunication = createCommunication(
            patientFhirId = "PatientFhirId",
            topic = "From Ronin",
            category = "alert",
            priority = "routine",
            sender = Reference(reference = "Practitioner/SendingPractitioner".asFHIR()),
            recipients = listOf(
                Reference(reference = "Practitioner/Recipient1".asFHIR()),
                Reference(reference = "Practitioner/Recipient2".asFHIR())
            ),
            encodedText = "VGhpcyBpcyBhIHNpbXBsZSBtZXNzYWdlLg=="
        )
        coEvery { cernerClient.post(tenant, "/Communication", expectedCommunication) } returns mockk {
            every { httpResponse.headers } returns mockk {
                every { this@mockk["Location"] } returns "Communication/1-2-3-4"
            }
        }

        val response = messageService.sendMessage(tenant, messageInput)
        assertEquals("1-2-3-4", response)
    }

    @Test
    fun `uses default messageTopic when none provided on vendor`() {
        val tenant = createTestTenant(
            mnemonic = "test",
            practitioner = "SendingPractitioner",
            messageTopic = null,
            messageCategory = "alert",
            messagePriority = "routine"
        )
        val messageInput = EHRMessageInput(
            text = "This is a simple message.",
            "PatientFhirId",
            listOf(EHRRecipient("Recipient1", IdVendorIdentifier(Id("Recipient1"))))
        )

        val expectedCommunication = createCommunication(
            patientFhirId = "PatientFhirId",
            topic = "Ronin Symptoms Alert",
            category = "alert",
            priority = "routine",
            sender = Reference(reference = "Practitioner/SendingPractitioner".asFHIR()),
            recipients = listOf(Reference(reference = "Practitioner/Recipient1".asFHIR())),
            encodedText = "VGhpcyBpcyBhIHNpbXBsZSBtZXNzYWdlLg=="
        )
        coEvery { cernerClient.post(tenant, "/Communication", expectedCommunication) } returns mockk {
            every { httpResponse.headers } returns mockk {
                every { this@mockk["Location"] } returns "Communication/1-2-3-4"
            }
        }

        val response = messageService.sendMessage(tenant, messageInput)
        assertEquals("1-2-3-4", response)
    }

    @Test
    fun `uses default messageCategory when none provided on vendor`() {
        val tenant = createTestTenant(
            mnemonic = "test",
            practitioner = "SendingPractitioner",
            messageTopic = "From Ronin",
            messageCategory = null,
            messagePriority = "routine"
        )
        val messageInput = EHRMessageInput(
            text = "This is a simple message.",
            "PatientFhirId",
            listOf(EHRRecipient("Recipient1", IdVendorIdentifier(Id("Recipient1"))))
        )

        val expectedCommunication = createCommunication(
            patientFhirId = "PatientFhirId",
            topic = "From Ronin",
            category = "notification",
            priority = "routine",
            sender = Reference(reference = "Practitioner/SendingPractitioner".asFHIR()),
            recipients = listOf(Reference(reference = "Practitioner/Recipient1".asFHIR())),
            encodedText = "VGhpcyBpcyBhIHNpbXBsZSBtZXNzYWdlLg=="
        )
        coEvery { cernerClient.post(tenant, "/Communication", expectedCommunication) } returns mockk {
            every { httpResponse.headers } returns mockk {
                every { this@mockk["Location"] } returns "Communication/1-2-3-4"
            }
        }

        val response = messageService.sendMessage(tenant, messageInput)
        assertEquals("1-2-3-4", response)
    }

    @Test
    fun `uses default messagePriority when none provided on vendor`() {
        val tenant = createTestTenant(
            mnemonic = "test",
            practitioner = "SendingPractitioner",
            messageTopic = "From Ronin",
            messageCategory = "alert",
            messagePriority = null
        )
        val messageInput = EHRMessageInput(
            text = "This is a simple message.",
            "PatientFhirId",
            listOf(EHRRecipient("Recipient1", IdVendorIdentifier(Id("Recipient1"))))
        )

        val expectedCommunication = createCommunication(
            patientFhirId = "PatientFhirId",
            topic = "From Ronin",
            category = "alert",
            priority = "urgent",
            sender = Reference(reference = "Practitioner/SendingPractitioner".asFHIR()),
            recipients = listOf(Reference(reference = "Practitioner/Recipient1".asFHIR())),
            encodedText = "VGhpcyBpcyBhIHNpbXBsZSBtZXNzYWdlLg=="
        )
        coEvery { cernerClient.post(tenant, "/Communication", expectedCommunication) } returns mockk {
            every { httpResponse.headers } returns mockk {
                every { this@mockk["Location"] } returns "Communication/1-2-3-4"
            }
        }

        val response = messageService.sendMessage(tenant, messageInput)
        assertEquals("1-2-3-4", response)
    }

    private fun createCommunication(
        patientFhirId: String,
        topic: String,
        category: String,
        priority: String,
        recipients: List<Reference>,
        sender: Reference,
        encodedText: String
    ): Communication = Communication(
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
        sender = sender,
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
