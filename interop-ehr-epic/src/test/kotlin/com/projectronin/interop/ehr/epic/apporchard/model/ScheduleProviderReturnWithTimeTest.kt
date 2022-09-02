package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScheduleProviderReturnWithTimeTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val scheduleProviderReturnWithTime = ScheduleProviderReturnWithTime(
            departmentIDs = listOf(
                IDType(id = "123", type = "Internal")
            ),
            departmentName = "Department name",
            duration = "45",
            providerIDs = listOf(
                IDType(id = "456", type = "Internal")
            ),
            providerName = "Provider Name",
            time = "3:30 PM"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(scheduleProviderReturnWithTime)

        val expectedJson = """
            {
              "DepartmentIDs" : [ {
                "ID" : "123",
                "Type" : "Internal"
              } ],
              "DepartmentName" : "Department name",
              "Duration" : "45",
              "ProviderIDs" : [ {
                "ID" : "456",
                "Type" : "Internal"
              } ],
              "ProviderName" : "Provider Name",
              "Time" : "3:30 PM"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedScheduleProviderReturnWithTime =
            JacksonManager.objectMapper.readValue<ScheduleProviderReturnWithTime>(json)
        assertEquals(scheduleProviderReturnWithTime, deserializedScheduleProviderReturnWithTime)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val scheduleProviderReturnWithTime = ScheduleProviderReturnWithTime(
            departmentIDs = listOf(),
            departmentName = "Department name",
            duration = "45",
            providerIDs = listOf(),
            providerName = "Provider Name",
            time = "3:30 PM"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(scheduleProviderReturnWithTime)

        val expectedJson = """
            {
              "DepartmentName" : "Department name",
              "Duration" : "45",
              "ProviderName" : "Provider Name",
              "Time" : "3:30 PM"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
              "DepartmentName" : "Department name",
              "Duration" : "45",
              "ProviderName" : "Provider Name",
              "Time" : "3:30 PM"
            }
        """.trimIndent()
        val scheduleProviderReturnWithTime = JacksonManager.objectMapper.readValue<ScheduleProviderReturnWithTime>(json)

        assertEquals(listOf<IDType>(), scheduleProviderReturnWithTime.departmentIDs)
        assertEquals(listOf<IDType>(), scheduleProviderReturnWithTime.providerIDs)
    }
}
