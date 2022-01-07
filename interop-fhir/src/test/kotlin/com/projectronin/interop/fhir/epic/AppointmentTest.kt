package com.projectronin.interop.fhir.epic

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.Provider

class AppointmentTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val appointment = Appointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "No Show",
            contactIDs = listOf(
                IdType(id = "12345", type = "ASN")
            ),
            date = "4/30/2015",
            extraExtensions = listOf(
                ExtensionInformationReturn(
                    extensionIds = listOf(
                        IdType(id = "abc", type = "type")
                    ),
                    extensionName = "extension name",
                    lines = listOf(
                        LineValue(
                            lineNumber = 1,
                            subLines = listOf(
                                SubLine(
                                    subLineNumber = 2,
                                    value = "subline value"
                                )
                            ),
                            value = "line value"
                        )
                    ),
                    value = "extension value"
                )
            ),
            extraItems = listOf(
                ItemValue(
                    itemNumber = "number",
                    lines = listOf(
                        LineValue(
                            lineNumber = 1,
                            subLines = listOf(
                                SubLine(
                                    subLineNumber = 2,
                                    value = "subline value"
                                )
                            ),
                            value = "line value"
                        )
                    ),
                    value = "item value"
                )
            ),
            patientIDs = listOf(
                IdType(id = "54321", type = "Internal")
            ),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IdType(id = "6789", type = "Internal")
                    ),
                    departmentName = "Test department",
                    duration = "30",
                    providerIDs = listOf(
                        IdType(id = "9876", type = "Internal")
                    ),
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(
                IdType(id = "abcd", type = "Internal")
            ),
            visitTypeName = "Test visit type"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appointment)

        val expectedJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentNotes" : [ "Notes" ],
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "No Show",
              "ContactIDs" : [ {
                "ID" : "12345",
                "Type" : "ASN"
              } ],
              "Date" : "4/30/2015",
              "ExtraExtensions" : [ {
                "ExtensionIds" : [ {
                  "ID" : "abc",
                  "Type" : "type"
                } ],
                "ExtensionName" : "extension name",
                "Lines" : [ {
                  "LineNumber" : 1,
                  "SubLines" : [ {
                    "SubLineNumber" : 2,
                    "Value" : "subline value"
                  } ],
                  "Value" : "line value"
                } ],
                "Value" : "extension value"
              } ],
              "ExtraItems" : [ {
                "ItemNumber" : "number",
                "Lines" : [ {
                  "LineNumber" : 1,
                  "SubLines" : [ {
                    "SubLineNumber" : 2,
                    "Value" : "subline value"
                  } ],
                  "Value" : "line value"
                } ],
                "Value" : "item value"
              } ],
              "PatientIDs" : [ {
                "ID" : "54321",
                "Type" : "Internal"
              } ],
              "PatientName" : "Test Name",
              "Providers" : [ {
                "DepartmentIDs" : [ {
                  "ID" : "6789",
                  "Type" : "Internal"
                } ],
                "DepartmentName" : "Test department",
                "Duration" : "30",
                "ProviderIDs" : [ {
                  "ID" : "9876",
                  "Type" : "Internal"
                } ],
                "ProviderName" : "Test Doc",
                "Time" : "3:30 PM"
              } ],
              "VisitTypeIDs" : [ {
                "ID" : "abcd",
                "Type" : "Internal"
              } ],
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedAppointment = JacksonManager.objectMapper.readValue<Appointment>(json)
        assertEquals(appointment, deserializedAppointment)
    }

    @Test
    fun `check that computed fields are returned`() {
        val appointment = Appointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "No Show",
            contactIDs = listOf(
                IdType(id = "12345", type = "ASN")
            ),
            date = "4/30/2015",
            extraExtensions = listOf(),
            extraItems = listOf(),
            patientIDs = listOf(
                IdType(id = "12345", type = "Internal")
            ),
            patientName = "Test Name",
            providers = listOf(),
            visitTypeIDs = listOf(),
            visitTypeName = "Test visit type"
        )

        assertEquals("12345", appointment.patientId)
        assertEquals("12345", appointment.id)
    }

    @Test
    fun `check that computed fields return null when not returned`() {
        val appointment = Appointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "No Show",
            contactIDs = listOf(
                IdType(id = "12345", type = "ASN")
            ),
            date = "4/30/2015",
            extraExtensions = listOf(),
            extraItems = listOf(),
            patientIDs = listOf(
                IdType(id = "12345", type = "bad-Internal")
            ),
            patientName = "Test Name",
            providers = listOf(),
            visitTypeIDs = listOf(),
            visitTypeName = "Test visit type"
        )

        assertNull(appointment.patientId)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val appointment = Appointment(
            appointmentDuration = "30",
            appointmentNotes = listOf(),
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "No Show",
            contactIDs = listOf(),
            date = "4/30/2015",
            extraExtensions = listOf(),
            extraItems = null,
            patientIDs = listOf(),
            patientName = "Test Name",
            providers = listOf(),
            visitTypeIDs = listOf(),
            visitTypeName = "Test visit type"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appointment)

        val expectedJson = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "No Show",
              "Date" : "4/30/2015",
              "PatientName" : "Test Name",
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
              "AppointmentDuration" : "30",
              "AppointmentStartTime" : "3:30 PM",
              "AppointmentStatus" : "No Show",
              "Date" : "4/30/2015",
              "PatientName" : "Test Name",
              "VisitTypeName" : "Test visit type"
            }
        """.trimIndent()
        val appointment = JacksonManager.objectMapper.readValue<Appointment>(json)

        assertEquals(listOf<String>(), appointment.appointmentNotes)
        assertEquals(listOf<IdType>(), appointment.contactIDs)
        assertEquals(listOf<ExtensionInformationReturn>(), appointment.extraExtensions)
        assertEquals(listOf<ItemValue>(), appointment.extraItems)
        assertEquals(listOf<IdType>(), appointment.patientIDs)
        assertEquals(listOf<Provider>(), appointment.providers)
        assertEquals(listOf<IdType>(), appointment.visitTypeIDs)
    }

    @Test
    fun `fails if we attempt to access an id that doesn't exist`() {
        val appointment = Appointment(
            appointmentDuration = "30",
            appointmentNotes = listOf(),
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "No Show",
            contactIDs = listOf(),
            date = "4/30/2015",
            extraExtensions = listOf(),
            extraItems = null,
            patientIDs = listOf(),
            patientName = "Test Name",
            providers = listOf(),
            visitTypeIDs = listOf(),
            visitTypeName = "Test visit type"
        )

        assertThrows<NoSuchElementException> {
            appointment.id
        }
    }
}
