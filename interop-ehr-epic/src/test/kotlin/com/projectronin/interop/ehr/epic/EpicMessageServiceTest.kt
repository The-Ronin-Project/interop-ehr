package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.IDType
import com.projectronin.interop.ehr.epic.model.SendMessageRecipient
import com.projectronin.interop.ehr.epic.model.SendMessageRequest
import com.projectronin.interop.ehr.epic.model.SendMessageResponse
import com.projectronin.interop.ehr.inputs.EHRMessageInput
import com.projectronin.interop.ehr.inputs.EHRRecipient
import io.ktor.client.call.receive
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
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
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()

    @BeforeEach
    fun initTest() {
        epicClient = mockk<EpicClient>()
        httpResponse = mockk<HttpResponse>()
    }

    @Test
    fun `ensure message can be sent`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT", "USER#1", "Symptom Alert"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<SendMessageResponse>() } returns SendMessageResponse(
            listOf(
                IDType(
                    "130375",
                    "Type"
                )
            )
        )
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2014/Common/Utility/SENDMESSAGE/Message",
                SendMessageRequest(
                    patientID = "MRN#1",
                    recipients = listOf(SendMessageRecipient("PROV#1", false)),
                    messageText = "Message Text",
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        val messageId =
            EpicMessageService(epicClient).sendMessage(
                tenant,
                EHRMessageInput(
                    "Message Text", "MRN#1",
                    listOf(EHRRecipient("PROV#1", false))
                )
            )

        assertEquals("130375", messageId)
    }

    @Test
    fun `ensure http error handled`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT", "USER#1", "Symptom Alert"
            )

        every { httpResponse.status } returns HttpStatusCode.NotFound
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2014/Common/Utility/SENDMESSAGE/Message",
                SendMessageRequest(
                    patientID = "MRN#1",
                    recipients = listOf(SendMessageRecipient("PROV#1", false)),
                    messageText = "Message Text",
                    senderID = "USER#1",
                    messageType = "Symptom Alert"
                )
            )
        } returns httpResponse

        assertThrows<IOException> {
            EpicMessageService(epicClient).sendMessage(
                tenant,
                EHRMessageInput(
                    "Message Text", "MRN#1",
                    listOf(EHRRecipient("PROV#1", false))
                )
            )
        }
    }
}
