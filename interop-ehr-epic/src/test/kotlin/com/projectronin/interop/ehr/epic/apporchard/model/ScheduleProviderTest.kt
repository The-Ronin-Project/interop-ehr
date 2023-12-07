package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScheduleProviderTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val scheduleProvider =
            ScheduleProvider(
                id = "E1000",
                idType = "external",
                departmentID = "",
                departmentIDType = "",
            )
        val actualJson = jacksonObjectMapper().writeValueAsString(scheduleProvider)

        assertEquals(
            """{"ID":"E1000","IDType":"external","DepartmentID":"","DepartmentIDType":""}""",
            actualJson,
        )

        val deserializedScheduleProvider = JacksonManager.objectMapper.readValue<ScheduleProvider>(actualJson)
        assertEquals(scheduleProvider, deserializedScheduleProvider)
    }

    @Test
    fun `can serialize and deserialize with default values`() {
        val scheduleProvider =
            ScheduleProvider(
                id = "E1000",
            )
        val actualJson = jacksonObjectMapper().writeValueAsString(scheduleProvider)

        assertEquals(
            """{"ID":"E1000","IDType":"External","DepartmentID":"","DepartmentIDType":""}""",
            actualJson,
        )

        val deserializedScheduleProvider = JacksonManager.objectMapper.readValue<ScheduleProvider>(actualJson)
        assertEquals(scheduleProvider, deserializedScheduleProvider)
    }
}
