package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetProviderAppointmentRequestTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val getProviderAppointmentRequest = GetProviderAppointmentRequest(
            userID = "1",
            userIDType = "External",
            startDate = "t-7",
            endDate = "t",
            combineDepartments = "true",
            resourceType = "",
            specialty = "",
            extraItems = listOf(),
            providers = listOf(
                ScheduleProvider(
                    id = "E1000",
                    idType = "external",
                    departmentID = "",
                    departmentIDType = ""
                )
            ),
            departments = listOf(),
            subgroups = listOf(),
            extraExtensions = listOf()
        )
        val actualJson =
            jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(getProviderAppointmentRequest)

        val expectedJson = """
            {
              "UserID" : "1",
              "UserIDType" : "External",
              "StartDate" : "t-7",
              "EndDate" : "t",
              "CombineDepartments" : "true",
              "ResourceType" : "",
              "Specialty" : "",
              "ExtraItems" : [ ],
              "Providers" : [ {
                "ID" : "E1000",
                "IDType" : "external",
                "DepartmentID" : "",
                "DepartmentIDType" : ""
              } ],
              "Departments" : [ ],
              "Subgroups" : [ ],
              "ExtraExtensions" : [ ]
            }
        """.trimIndent()
        assertEquals(expectedJson, actualJson)

        val deserializedProviderAppointmentRequest =
            JacksonManager.objectMapper.readValue<GetProviderAppointmentRequest>(actualJson)
        assertEquals(getProviderAppointmentRequest, deserializedProviderAppointmentRequest)
    }

    @Test
    fun `can serialize and deserialize with default values`() {
        val getProviderAppointmentRequest = GetProviderAppointmentRequest(
            userID = "1",
            startDate = "t-7",
            endDate = "t",
            providers = listOf(
                ScheduleProvider(
                    id = "E1000",
                    idType = "external"
                )
            )
        )
        val actualJson =
            jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(getProviderAppointmentRequest)

        val expectedJson = """
            {
              "UserID" : "1",
              "UserIDType" : "External",
              "StartDate" : "t-7",
              "EndDate" : "t",
              "CombineDepartments" : "true",
              "ResourceType" : "",
              "Specialty" : "",
              "ExtraItems" : [ ],
              "Providers" : [ {
                "ID" : "E1000",
                "IDType" : "external",
                "DepartmentID" : "",
                "DepartmentIDType" : ""
              } ],
              "Departments" : [ ],
              "Subgroups" : [ ],
              "ExtraExtensions" : [ ]
            }
        """.trimIndent()
        assertEquals(expectedJson, actualJson)

        val deserializedProviderAppointmentRequest =
            JacksonManager.objectMapper.readValue<GetProviderAppointmentRequest>(actualJson)
        assertEquals(getProviderAppointmentRequest, deserializedProviderAppointmentRequest)
    }
}
