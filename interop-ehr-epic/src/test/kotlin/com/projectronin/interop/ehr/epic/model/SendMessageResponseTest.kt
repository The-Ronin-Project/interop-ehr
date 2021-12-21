package com.projectronin.interop.ehr.epic.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SendMessageResponseTest {
    @Test
    fun `ensure json deserialization works`() {
        val json = """{"IDTypes": [{"ID": "130633", "Type": "Numeric"}]}"""
        val sendMessageResponse = jacksonObjectMapper().readValue(json, SendMessageResponse::class.java)

        assertEquals(1, sendMessageResponse.iDTypes.size)
        assertEquals("130633", sendMessageResponse.iDTypes[0].iD)
        assertEquals("Numeric", sendMessageResponse.iDTypes[0].type)
    }
}
