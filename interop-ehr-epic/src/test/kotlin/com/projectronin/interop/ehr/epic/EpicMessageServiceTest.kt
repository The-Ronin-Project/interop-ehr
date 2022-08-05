package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRecipient
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageResponse
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.EHRMessageInput
import com.projectronin.interop.ehr.inputs.EHRRecipient
import com.projectronin.interop.ehr.inputs.IdVendorIdentifier
import com.projectronin.interop.ehr.inputs.IdentifierVendorIdentifier
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.tenant.config.ProviderPoolService
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicMessageServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var httpResponse: HttpResponse
    private lateinit var providerPoolService: ProviderPoolService
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()

    @BeforeEach
    fun initTest() {
        epicClient = mockk()
        httpResponse = mockk()
        providerPoolService = mockk()
    }

    @Test
    fun `ensure message can be sent`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            "Test Tenant",
            "USER#1",
            "Symptom Alert"
        )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<SendMessageResponse>() } returns SendMessageResponse(
            listOf(
                IDType(
                    "130375", "Type"
                )
            )
        )
        coEvery {
            epicClient.post(
                tenant, "/api/epic/2014/Common/Utility/SENDMESSAGE/Message",
                SendMessageRequest(
                    patientID = "MRN#1",
                    recipients = listOf(SendMessageRecipient("CorrectID", false)),
                    messageText = listOf("Message Text"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID"))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text", "MRN#1", recipientsList
            )
        )

        assertEquals("130375", messageId)
    }

    @Test
    fun `ensure multi-line message can be sent`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            "Test Tenant",
            "USER#1",
            "Symptom Alert"
        )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<SendMessageResponse>() } returns SendMessageResponse(
            listOf(
                IDType(
                    "130375", "Type"
                )
            )
        )
        coEvery {
            epicClient.post(
                tenant, "/api/epic/2014/Common/Utility/SENDMESSAGE/Message",
                SendMessageRequest(
                    patientID = "MRN#1",
                    recipients = listOf(SendMessageRecipient("CorrectID", false)),
                    messageText = listOf("Message Text", "Line 2", "", "Line 4"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID"))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text\nLine 2\n\nLine 4", "MRN#1", recipientsList
            )
        )

        assertEquals("130375", messageId)
    }

    @Test
    fun `ensure pool ID works can be sent`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            "Test Tenant",
            "USER#1",
            "Symptom Alert"
        )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<SendMessageResponse>() } returns SendMessageResponse(
            listOf(
                IDType(
                    "130375", "Type"
                )
            )
        )
        coEvery {
            epicClient.post(
                tenant, "/api/epic/2014/Common/Utility/SENDMESSAGE/Message",
                SendMessageRequest(
                    patientID = "MRN#1",
                    recipients = listOf(SendMessageRecipient("PoolID", true)), // this is an implied assertion
                    messageText = listOf("Message Text"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID"))
            )
        )

        every {
            providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID"))
        } returns mapOf("CorrectID" to "PoolID")

        val messageId = EpicMessageService(epicClient, providerPoolService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text", "MRN#1", recipientsList
            )
        )

        assertEquals("130375", messageId)
    }

    @Test
    fun `ensure wrong identifier types fail`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            "USER#1",
            "Symptom Alert"
        )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<SendMessageResponse>() } returns SendMessageResponse(
            listOf(
                IDType(
                    "130375", "Type"
                )
            )
        )
        coEvery {
            epicClient.post(
                tenant, "/api/epic/2014/Common/Utility/SENDMESSAGE/Message",
                SendMessageRequest(
                    patientID = "MRN#1",
                    recipients = listOf(), // this is an implied assertion
                    messageText = listOf("Message Text"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdVendorIdentifier(Id("1234"))
            )
        )

        assertThrows<ClassCastException> {
            EpicMessageService(epicClient, providerPoolService).sendMessage(
                tenant,
                EHRMessageInput(
                    "Message Text", "MRN#1", recipientsList
                )
            )
        }
    }

    @Test
    fun `ensure identifier without value fails`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            "USER#1",
            "Symptom Alert"
        )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<SendMessageResponse>() } returns SendMessageResponse(
            listOf(
                IDType(
                    "130375", "Type"
                )
            )
        )
        coEvery {
            epicClient.post(
                tenant, "/api/epic/2014/Common/Utility/SENDMESSAGE/Message",
                SendMessageRequest(
                    patientID = "MRN#1",
                    recipients = listOf(), // this is an implied assertion
                    messageText = listOf("Message Text"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = null))
            )
        )

        assertThrows<VendorIdentifierNotFoundException> {
            EpicMessageService(epicClient, providerPoolService).sendMessage(
                tenant,
                EHRMessageInput(
                    "Message Text", "MRN#1", recipientsList
                )
            )
        }
    }

    @Test
    fun `ensure other error handled`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            "Test Tenant",
            "USER#1",
            "Symptom Alert"
        )

        coEvery {
            epicClient.post(
                tenant, "/api/epic/2014/Common/Utility/SENDMESSAGE/Message",
                SendMessageRequest(
                    patientID = "MRN#1",
                    recipients = listOf(SendMessageRecipient("CorrectID", false)),
                    messageText = listOf("Message Text"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } throws Exception("something went wrong")

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID"))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        assertThrows<Exception> {
            EpicMessageService(epicClient, providerPoolService).sendMessage(
                tenant,
                EHRMessageInput(
                    "Message Text", "MRN#1", recipientsList
                )
            )
        }
    }
}
