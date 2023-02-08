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
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.ProviderPoolService
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.opentracing.Span
import io.opentracing.util.GlobalTracer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.aidbox.PatientService as AidboxPatientService

class EpicMessageServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var httpResponse: HttpResponse
    private lateinit var providerPoolService: ProviderPoolService
    private lateinit var aidboxPatientService: AidboxPatientService
    private lateinit var identifierService: EpicIdentifierService
    private lateinit var span: Span
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()

    @BeforeEach
    fun initTest() {
        epicClient = mockk()
        httpResponse = mockk()
        aidboxPatientService = mockk()
        identifierService = mockk()
        providerPoolService = mockk()

        span = mockk(relaxed = true)

        mockkStatic(GlobalTracer::class)
        every { GlobalTracer.get() } returns mockk {
            every { activeSpan() } returns span
        }
        val mockIdentifier = mockk<Identifier> {
            every { value } returns mockk {
                every { value } returns "MRN#1"
            }
        }
        val mockIdentifierList = listOf(mockIdentifier)
        val mockPatient = mockk<Patient> {
            every { identifier } returns mockIdentifierList
        }

        every { aidboxPatientService.getPatientByFHIRId(any(), any()) } returns mockPatient
        every { identifierService.getMRNIdentifier(any(), mockIdentifierList) } returns mockIdentifier
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
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
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text", "fhirId1", recipientsList
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
                    messageText = listOf("Message Text", "Line 2", " ", "Line 4"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text\nLine 2\n\nLine 4", "fhirId1", recipientsList
            )
        )

        assertEquals("130375", messageId)
    }

    @Test
    fun `ensure multi-line messages with carriage returns can be sent`() {
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
                    messageText = listOf("Message Text", "Line 2", " ", "Line 4"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text\r\nLine 2\r\n\r\nLine 4", "fhirId1", recipientsList
            )
        )

        assertEquals("130375", messageId)
    }

    @Test
    fun `ensure multi-line messages with mixed newlines and carriage returned newlines can be sent`() {
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
                    messageText = listOf("Message Text", "Line 2", " ", "Line 4"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text\r\nLine 2\n\nLine 4", "fhirId1", recipientsList
            )
        )

        assertEquals("130375", messageId)
    }

    @Test
    fun `ensure multi-line messages preserve blank lines using spaces - newlines only`() {

        // test private fun translateMessageInput() messageText input processing logic

        val messageInput = "\n\nMessage Text\n\nLine 2\nLine 3\nLine 4\n\n\nLine 7\n\nLine 8\n"
        val messageText = messageInput.split("\r\n", "\n").map { if (it.isEmpty()) " " else it }
        assertEquals(
            listOf(" ", " ", "Message Text", " ", "Line 2", "Line 3", "Line 4", " ", " ", "Line 7", " ", "Line 8", " "),
            messageText
        )

        // now do the unit test with mocking

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
                    messageText = messageText,
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(messageInput, "fhirId1", recipientsList)
        )

        assertEquals("130375", messageId)
    }

    @Test
    fun `ensure multi-line messages preserve blank lines using spaces - carriage returned newlines only`() {

        // test private fun translateMessageInput() messageText input processing logic

        val messageInput = "\r\n\r\nMessage Text\r\n\r\nLine 2\r\nLine 3\r\nLine 4\r\n\r\n\r\nLine 7\r\n\r\nLine 8\r\n"
        val messageText = messageInput.split("\r\n", "\n").map { if (it.isEmpty()) " " else it }
        assertEquals(
            listOf(" ", " ", "Message Text", " ", "Line 2", "Line 3", "Line 4", " ", " ", "Line 7", " ", "Line 8", " "),
            messageText
        )

        // now do the unit test with mocking

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
                    messageText = messageText,
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(messageInput, "fhirId1", recipientsList)
        )

        assertEquals("130375", messageId)
    }

    @Test
    fun `ensure multi-line messages preserve blank lines using spaces - mixed newlines and carriage returned newlines`() {

        // test private fun translateMessageInput() messageText input processing logic

        val messageInput = "\r\n\nMessage Text\n\r\nLine 2\r\nLine 3\nLine 4\n\r\n\nLine 7\r\n\r\nLine 8\n"
        val messageText = messageInput.split("\r\n", "\n").map { if (it.isEmpty()) " " else it }
        assertEquals(
            listOf(" ", " ", "Message Text", " ", "Line 2", "Line 3", "Line 4", " ", " ", "Line 7", " ", "Line 8", " "),
            messageText
        )

        // now do the unit test with mocking

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
                    messageText = messageText,
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(messageInput, "fhirId1", recipientsList)
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
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every {
            providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID"))
        } returns mapOf("CorrectID" to "PoolID")

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text", "fhirId1", recipientsList
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
            EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
                tenant,
                EHRMessageInput(
                    "Message Text", "fhirId1", recipientsList
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
            EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
                tenant,
                EHRMessageInput(
                    "Message Text", "fhirId1", recipientsList
                )
            )
        }
    }

    @Test
    fun `ensure identifier with value with null value fails`() {
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
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = FHIRString(null)))
            )
        )

        assertThrows<VendorIdentifierNotFoundException> {
            EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
                tenant,
                EHRMessageInput(
                    "Message Text", "fhirId1", recipientsList
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
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        assertThrows<Exception> {
            EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
                tenant,
                EHRMessageInput(
                    "Message Text", "fhirId1", recipientsList
                )
            )
        }
    }

    @Test
    fun `ensure span is configured when present and recipient is user`() {
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
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text", "fhirId1", recipientsList
            )
        )

        assertEquals("130375", messageId)

        verify(exactly = 1) { span.setTag("message.recipients", "user:CorrectID") }
        verify(exactly = 1) { span.setTag("message.responseId", "Type:130375") }
    }

    @Test
    fun `ensure span is configured when present and recipient is pool`() {
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
                    recipients = listOf(SendMessageRecipient("PoolID", true)),
                    messageText = listOf("Message Text"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every {
            providerPoolService.getPoolsForProviders(
                tenant,
                listOf("CorrectID")
            )
        } returns mapOf("CorrectID" to "PoolID")

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text", "fhirId1", recipientsList
            )
        )

        assertEquals("130375", messageId)

        verify(exactly = 1) { span.setTag("message.recipients", "pool:PoolID") }
        verify(exactly = 1) { span.setTag("message.responseId", "Type:130375") }
    }

    @Test
    fun `ensure span is configured when present and multiple recipients`() {
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
                    recipients = listOf(
                        SendMessageRecipient("CorrectID", false),
                        SendMessageRecipient("PoolID", true)
                    ),
                    messageText = listOf("Message Text"),
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            ),
            EHRRecipient(
                "PROV#2",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID2".asFHIR()))
            )
        )

        every {
            providerPoolService.getPoolsForProviders(
                tenant,
                listOf("CorrectID", "CorrectID2")
            )
        } returns mapOf("CorrectID2" to "PoolID")

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text", "fhirId1", recipientsList
            )
        )

        assertEquals("130375", messageId)

        verify(exactly = 1) { span.setTag("message.recipients", "user:CorrectID, pool:PoolID") }
        verify(exactly = 1) { span.setTag("message.responseId", "Type:130375") }
    }

    @Test
    fun `ensure span is ignored when no active span`() {
        every { GlobalTracer.get() } returns mockk {
            every { activeSpan() } returns null
        }

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
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        val messageId = EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
            tenant,
            EHRMessageInput(
                "Message Text", "fhirId1", recipientsList
            )
        )

        assertEquals("130375", messageId)

        verify { span wasNot Called }
    }

    @Test
    fun `ensure aidbox bad mrn fails`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            "Test Tenant",
            "USER#1",
            "Symptom Alert"
        )
        val mockIdentifier = mockk<Identifier> {
            every { value } returns null
        }
        val mockIdentifierList = listOf(mockIdentifier)
        val mockPatient = mockk<Patient> {
            every { identifier } returns mockIdentifierList
        }

        every { aidboxPatientService.getPatientByFHIRId("TEST_TENANT", "badId") } returns mockPatient
        every { identifierService.getMRNIdentifier(tenant, mockIdentifierList) } returns mockIdentifier

        val recipientsList = listOf(
            EHRRecipient(
                "PROV#1",
                IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "CorrectID".asFHIR()))
            )
        )

        every { providerPoolService.getPoolsForProviders(tenant, listOf("CorrectID")) } returns emptyMap()

        assertThrows<VendorIdentifierNotFoundException> {
            EpicMessageService(epicClient, providerPoolService, aidboxPatientService, identifierService).sendMessage(
                tenant,
                EHRMessageInput(
                    "Message Text", "badId", recipientsList
                )
            )
        }
    }
}
