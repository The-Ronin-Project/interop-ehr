package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SendMessageRequestTest {
    @Test
    fun `ensure json serialization conventions are correct`() {
        val sendMessageString =
            jacksonObjectMapper().writeValueAsString(
                SendMessageRequest(
                    messageText = "Message Text",
                    patientID = "MRN#1",
                    patientIDType = "MRN",
                    recipients = listOf(),
                    senderID = "Sender#1",
                    senderIDType = "SendType#1",
                    messageType = "MessageType",
                    contactID = "Con#1",
                    contactIDType = "ConType#1"
                )
            )

        assertEquals(
            """{"MessageText":"Message Text","PatientID":"MRN#1","Recipients":[],"SenderID":"Sender#1","MessageType":"MessageType","SenderIDType":"SendType#1","PatientIDType":"MRN","ContactID":"Con#1","ContactIDType":"ConType#1","MessagePriority":""}""",
            sendMessageString
        )
    }

    @Test
    fun `ensure defaults are correct`() {
        val sendMessageString =
            jacksonObjectMapper().writeValueAsString(
                SendMessageRequest(
                    messageText = "Message Text",
                    patientID = "MRN#1",
                    recipients = listOf(),
                    senderID = "Sender#1",
                    messageType = "Symptom Alerts"
                )
            )

        assertEquals(
            """{"MessageText":"Message Text","PatientID":"MRN#1","Recipients":[],"SenderID":"Sender#1","MessageType":"Symptom Alerts","SenderIDType":"External","PatientIDType":"MRN","ContactID":"","ContactIDType":"","MessagePriority":""}""",
            sendMessageString
        )
    }
}
