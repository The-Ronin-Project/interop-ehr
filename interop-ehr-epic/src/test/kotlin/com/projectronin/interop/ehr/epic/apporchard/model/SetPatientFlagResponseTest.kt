package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SetPatientFlagResponseTest {
    @Test
    fun `ensure json deserialization works`() {
        val json = """{"Error": "wow", "Success": false}"""
        val response = jacksonObjectMapper().readValue(json, SetPatientFlagResponse::class.java)

        assertEquals("wow", response.error)
        assertFalse(response.success)
    }
}
