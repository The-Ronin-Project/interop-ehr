package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.apporchard.model.Appointment
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicAppointmentBundleTest {
    @Test
    fun `can build from object`() {
        val visitIdentifier1 = IDType(id = "25000", type = "Internal")
        val visitIdentifier2 = IDType(id = "25001", type = "Internal")
        val getPatientAppointments = GetAppointmentsResponse(
            error = null,
            appointments = listOf(
                Appointment(
                    patientName = "LMRTESTING,HERMIONE",
                    date = "4/30/2015",
                    visitTypeName = "TRANSPLANT EVALUATION",
                    appointmentNotes = listOf(),
                    appointmentStartTime = " 3:30 PM",
                    appointmentDuration = "30",
                    appointmentStatus = "No Show",
                    visitTypeIDs = listOf(visitIdentifier1),
                    providers = listOf(),
                    extraItems = null,
                    extraExtensions = listOf(),
                    contactIDs = listOf(IDType("22792", "ASN"))
                ),
                Appointment(
                    patientName = "LMRTESTING,HERMIONE",
                    date = "4/29/2015",
                    visitTypeName = "INFUSION 120",
                    appointmentNotes = listOf("test"),
                    appointmentStartTime = "11:45 AM",
                    appointmentDuration = "120",
                    appointmentStatus = "Completed",
                    visitTypeIDs = listOf(visitIdentifier2),
                    providers = listOf(),
                    extraItems = null,
                    extraExtensions = listOf(),
                    contactIDs = listOf(IDType("22793", "ASN"))
                )
            )
        )

        val bundle = EpicAppointmentBundle(getPatientAppointments, emptyMap(), emptyMap())
        assertEquals(getPatientAppointments, bundle.resource)
        assertEquals(DataSource.EPIC_APPORCHARD, bundle.dataSource)
        assertEquals(0, bundle.links.size)

        val appointment1 = bundle.resources[0]
        assertEquals("22792", appointment1.id)
        assertEquals(visitIdentifier1, appointment1.identifier[0].element)
        assertEquals("25000", appointment1.identifier[0].value)
        assertEquals("Internal", appointment1.identifier[0].type?.text)
        assertEquals(null, appointment1.identifier[0].system)
        assertEquals("2015-04-30T15:30:00", appointment1.start)
        assertNull(appointment1.appointmentType)
        assertEquals("TRANSPLANT EVALUATION", appointment1.serviceType[0].text)
        assertEquals(AppointmentStatus.NOSHOW, appointment1.status)

        val appointment2 = bundle.resources[1]
        assertEquals("22793", appointment2.id)
        assertEquals(visitIdentifier2, appointment2.identifier[0].element)
        assertEquals("25001", appointment2.identifier[0].value)
        assertEquals("Internal", appointment2.identifier[0].type?.text)
        assertEquals(null, appointment2.identifier[0].system)
        assertEquals("2015-04-29T11:45:00", appointment2.start)
        assertNull(appointment2.appointmentType)
        assertEquals("INFUSION 120", appointment2.serviceType[0].text)
        assertEquals(AppointmentStatus.FULFILLED, appointment2.status)
    }

    @Test
    fun `ensure no appointments handled`() {
        val getPatientAppointments = GetAppointmentsResponse(
            error = null,
            appointments = listOf()
        )

        val bundle = EpicAppointmentBundle(getPatientAppointments, emptyMap(), emptyMap())
        assertEquals(getPatientAppointments, bundle.resource)
        assertEquals(DataSource.EPIC_APPORCHARD, bundle.dataSource)
        assertEquals(listOf<EpicAppointment>(), bundle.resources)
    }

    @Test
    fun `returns JSON as raw`() {
        val visitIdentifier1 = IDType(id = "25000", type = "Internal")
        val visitIdentifier2 = IDType(id = "25001", type = "Internal")
        val getPatientAppointments = GetAppointmentsResponse(
            error = null,
            appointments = listOf(
                Appointment(
                    patientName = "LMRTESTING,HERMIONE",
                    date = "4/30/2015",
                    visitTypeName = "TRANSPLANT EVALUATION",
                    appointmentNotes = listOf(),
                    appointmentStartTime = " 3:30 PM",
                    appointmentDuration = "30",
                    appointmentStatus = "No Show",
                    visitTypeIDs = listOf(visitIdentifier1),
                    providers = listOf(),
                    extraItems = null,
                    extraExtensions = listOf(),
                    contactIDs = listOf(IDType("22792", "ASN"))
                ),
                Appointment(
                    patientName = "LMRTESTING,HERMIONE",
                    date = "4/29/2015",
                    visitTypeName = "INFUSION 120",
                    appointmentNotes = listOf("test"),
                    appointmentStartTime = "11:45 AM",
                    appointmentDuration = "120",
                    appointmentStatus = "Completed",
                    visitTypeIDs = listOf(visitIdentifier2),
                    providers = listOf(),
                    extraItems = null,
                    extraExtensions = listOf(),
                    contactIDs = listOf(IDType("22793", "ASN"))
                )
            )
        )

        val visitIdentifierJson1 = """{"ID":"25000","Type":"Internal"}"""
        val visitIdentifierJson2 = """{"ID":"25001","Type":"Internal"}"""

        val json = """
        |{
        |  "Error": null,
        |  "Appointments": [
        |    {
        |      "AppointmentDuration": "30",
        |      "AppointmentNotes": [],
        |      "AppointmentStartTime": " 3:30 PM",
        |      "AppointmentStatus": "No Show",
        |      "ContactIDs": [ {
        |        "ID": "22792",
        |        "Type": "ASN"
        |      } ],
        |      "Date": "4/30/2015",
        |      "ExtraExtensions": [],
        |      "ExtraItems": null,
        |      "PatientName": "LMRTESTING,HERMIONE",
        |      "Providers": [],
        |      "VisitTypeIDs": [
        |        $visitIdentifierJson1
        |      ],
        |      "VisitTypeName": "TRANSPLANT EVALUATION"
        |    },
        |    {
        |    "AppointmentDuration": "120",
        |    "AppointmentNotes": [
        |      "test"
        |    ],
        |    "AppointmentStartTime": "11:45 AM",
        |    "AppointmentStatus": "Completed",
        |      "ContactIDs": [ {
        |        "ID": "22793",
        |        "Type": "ASN"
        |      } ],
        |    "Date": "4/29/2015",
        |    "ExtraExtensions": [],
        |    "ExtraItems": null,
        |    "PatientName": "LMRTESTING,HERMIONE",
        |    "Providers": [],
        |    "VisitTypeIDs": [
        |      $visitIdentifierJson2
        |    ],
        |    "VisitTypeName": "INFUSION 120"
        |    }
        |  ]
        |}
        """.trimMargin()

        val bundle = EpicAppointmentBundle(getPatientAppointments, emptyMap(), emptyMap())
        assertEquals(getPatientAppointments, bundle.resource)
        assertEquals(deformat(json), bundle.raw)

        val appointment1 = bundle.resources[0]
        assertEquals(visitIdentifier1, appointment1.identifier[0].element)
        assertEquals(visitIdentifierJson1, appointment1.identifier[0].raw)

        val appointment2 = bundle.resources[1]
        assertEquals(visitIdentifier2, appointment2.identifier[0].element)
        assertEquals(visitIdentifierJson2, appointment2.identifier[0].raw)
    }
}
