package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicAppointmentTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val appointment = EpicAppointment(
            contactIDs = listOf(
                IDType(id = "12345", type = "CSN")
            ),
            patientIDs = listOf(
                IDType(id = "54321", type = "Internal")
            ),
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appointment)

        val expectedJson = """
            {
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "CSN"
              } ],
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ]
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedAppointment = JacksonManager.objectMapper.readValue<EpicAppointment>(json)
        assertEquals(appointment, deserializedAppointment)
    }

    @Test
    fun `check that computed fields are returned`() {
        val appointment = EpicAppointment(
            contactIDs = listOf(
                IDType(id = "12345", type = "CSN")
            ),
            patientIDs = listOf(
                IDType(id = "12345", type = "Internal")
            ),
        )

        assertEquals("12345", appointment.patientId)
        assertEquals("12345", appointment.id)
    }

    @Test
    fun `check that computed fields return null when not returned`() {
        val appointment = EpicAppointment(
            contactIDs = listOf(
                IDType(id = "12345", type = "CSN")
            ),
            patientIDs = listOf(
                IDType(id = "12345", type = "bad-Internal")
            ),
        )

        assertNull(appointment.patientId)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val appointment = EpicAppointment(
            contactIDs = listOf(),
            patientIDs = listOf(),
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appointment)

        val expectedJson = """
            { }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with fields we don't care about`() {
        val json = """
            {
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "CSN"
              } ],
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "No Show",
              "Date" : "4/30/2015",
              "PatientName" : "Test Name",
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()
        val appointment = JacksonManager.objectMapper.readValue<EpicAppointment>(json)

        assertEquals("12345", appointment.id)
        assertEquals("54321", appointment.patientId)
    }

    @Test
    fun `fails if we attempt to access an id that doesn't exist`() {
        val appointment = EpicAppointment(
            contactIDs = listOf(),
            patientIDs = listOf(),
        )

        assertThrows<NoSuchElementException> {
            appointment.id
        }
    }
}
