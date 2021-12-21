package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicAppointmentTest {

    @Test
    fun `can build from JSON`() {
        val visitIdentifierJson = """{"ID":"25000","Type":"Internal"}"""

        val json = """{
        |  "PatientName": "LMRTESTING,HERMIONE",
        |  "Date": "12/3/2015",
        |  "VisitTypeName": "TRANSPLANT EVALUATION",
        |  "AppointmentNotes": [],
        |  "AppointmentStartTime": " 3:30 PM",
        |  "AppointmentDuration": "30",
        |  "AppointmentStatus": "No Show",
        |  "ContactIDs": [
        |   {
        |     "ID": "22792",
        |     "Type": "CSN"
        |   },
        |   {
        |     "ID": "22792",
        |     "Type": "ASN"
        |   }
        |  ],
        |  "VisitTypeIDs": [
        |    $visitIdentifierJson
        |  ],
        |  "Providers": [],
        |  "ExtraItems": null,
        |  "ExtraExtensions": []
        |}
        """.trimMargin()

        val appointment = EpicAppointment(json)
        assertEquals(json, appointment.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, appointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, appointment.resourceType)
        assertEquals("22792", appointment.id)
        assertEquals(visitIdentifierJson, appointment.identifier[0].raw)
        assertEquals("25000", appointment.identifier[0].value)
        assertEquals("Internal", appointment.identifier[0].system)
        assertEquals("2015-12-03T15:30:00", appointment.start)
        assertEquals("TRANSPLANT EVALUATION", appointment.serviceType[0].text)
        assertEquals(AppointmentStatus.NOSHOW, appointment.status)
    }

    @Test
    fun `handles 2 digit days`() {
        val visitIdentifierJson = """{"ID":"25000","Type":"Internal"}"""

        val json = """{
        |  "PatientName": "LMRTESTING,HERMIONE",
        |  "Date": "12/13/2015",
        |  "VisitTypeName": "TRANSPLANT EVALUATION",
        |  "AppointmentNotes": [],
        |  "AppointmentStartTime": " 3:30 PM",
        |  "AppointmentDuration": "30",
        |  "AppointmentStatus": "No Show",
        |  "ContactIDs": [
        |   {
        |     "ID": "22792",
        |     "Type": "CSN"
        |   },
        |   {
        |     "ID": "22792",
        |     "Type": "ASN"
        |   }
        |  ],
        |  "VisitTypeIDs": [
        |    $visitIdentifierJson
        |  ],
        |  "Providers": [],
        |  "ExtraItems": null,
        |  "ExtraExtensions": []
        |}
        """.trimMargin()

        val appointment = EpicAppointment(json)
        assertEquals(json, appointment.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, appointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, appointment.resourceType)
        assertEquals("22792", appointment.id)
        assertEquals(visitIdentifierJson, appointment.identifier[0].raw)
        assertEquals("25000", appointment.identifier[0].value)
        assertEquals("Internal", appointment.identifier[0].system)
        assertEquals("2015-12-13T15:30:00", appointment.start)
        assertEquals("TRANSPLANT EVALUATION", appointment.serviceType[0].text)
        assertEquals(AppointmentStatus.NOSHOW, appointment.status)
    }

    @Test
    fun `handles 1 digit months`() {
        val visitIdentifierJson = """{"ID":"25000","Type":"Internal"}"""

        val json = """{
        |  "PatientName": "LMRTESTING,HERMIONE",
        |  "Date": "1/13/2015",
        |  "VisitTypeName": "TRANSPLANT EVALUATION",
        |  "AppointmentNotes": [],
        |  "AppointmentStartTime": " 3:30 PM",
        |  "AppointmentDuration": "30",
        |  "AppointmentStatus": "No Show",
        |  "ContactIDs": [
        |   {
        |     "ID": "22792",
        |     "Type": "CSN"
        |   },
        |   {
        |     "ID": "22792",
        |     "Type": "ASN"
        |   }
        |  ],
        |  "VisitTypeIDs": [
        |    $visitIdentifierJson
        |  ],
        |  "Providers": [],
        |  "ExtraItems": null,
        |  "ExtraExtensions": []
        |}
        """.trimMargin()

        val appointment = EpicAppointment(json)
        assertEquals(json, appointment.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, appointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, appointment.resourceType)
        assertEquals("22792", appointment.id)
        assertEquals(visitIdentifierJson, appointment.identifier[0].raw)
        assertEquals("25000", appointment.identifier[0].value)
        assertEquals("Internal", appointment.identifier[0].system)
        assertEquals("2015-01-13T15:30:00", appointment.start)
        assertEquals("TRANSPLANT EVALUATION", appointment.serviceType[0].text)
        assertEquals(AppointmentStatus.NOSHOW, appointment.status)
    }
}
