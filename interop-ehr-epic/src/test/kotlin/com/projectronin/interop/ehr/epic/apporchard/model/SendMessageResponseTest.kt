package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SendMessageResponseTest {
    @Test
    fun `ensure json deserialization works`() {
        val json = """{"IDTypes": [{"ID": "130633", "Type": "Numeric"}]}"""
        val sendMessageResponse = jacksonObjectMapper().readValue(json, SendMessageResponse::class.java)

        assertEquals(1, sendMessageResponse.idTypes.size)
        assertEquals("130633", sendMessageResponse.idTypes[0].id)
        assertEquals("Numeric", sendMessageResponse.idTypes[0].type)
    }
}
