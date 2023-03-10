package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetPatientFlagRequestTest {
    @Test
    fun `ensure defaults are correct`() {
        val setPatientFlagRequest = SetPatientFlagRequest(PatientFlag(type = "123"))
        val json = jacksonObjectMapper().writeValueAsString(setPatientFlagRequest)

        assertEquals(
            """{"Flag":{"ID":null,"IDType":null,"Status":"1","Summary":null,"Text":null,"Type":"123"}}""",
            json
        )
    }

    @Test
    fun `ensure serialization works`() {
        val flag = PatientFlag(
            id = "123",
            idType = "External",
            status = null,
            summary = "Summary",
            text = listOf("First", "Second"),
            type = null
        )
        val setPatientFlagRequest = SetPatientFlagRequest(flag)
        val json = jacksonObjectMapper().writeValueAsString(setPatientFlagRequest)

        assertEquals(
            """{"Flag":{"ID":"123","IDType":"External","Status":null,"Summary":"Summary","Text":["First","Second"],"Type":null}}""",
            json
        )
    }
}
