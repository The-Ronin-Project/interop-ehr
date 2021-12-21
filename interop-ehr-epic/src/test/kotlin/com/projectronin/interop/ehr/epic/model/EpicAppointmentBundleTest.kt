package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicAppointmentBundleTest {

    @Test
    fun `can build from JSON`() {
        val visitIdentifierJson1 = """{"ID":"25000","Type":"Internal"}"""
        val visitIdentifierJson2 = """{"ID":"25001","Type":"Internal"}"""

        val json = """
        |{
        |  "Error": null,
        |  "Appointments": [
        |    {
        |      "PatientName": "LMRTESTING,HERMIONE",
        |      "Date": "4/30/2015",
        |      "VisitTypeName": "TRANSPLANT EVALUATION",
        |      "AppointmentNotes": [],
        |      "AppointmentStartTime": " 3:30 PM",
        |      "AppointmentDuration": "30",
        |      "AppointmentStatus": "No Show",
        |      "VisitTypeIDs": [
        |        $visitIdentifierJson1
        |      ],
        |      "Providers": [],
        |      "ExtraItems": null,
        |      "ExtraExtensions": []
        |    },
        |    {
        |    "PatientName": "LMRTESTING,HERMIONE",
        |    "Date": "4/29/2015",
        |    "VisitTypeName": "INFUSION 120",
        |    "AppointmentNotes": [
        |      "test"
        |    ],
        |    "AppointmentStartTime": "11:45 AM",
        |    "AppointmentDuration": "120",
        |    "AppointmentStatus": "Completed",
        |    "VisitTypeIDs": [
        |      $visitIdentifierJson2
        |    ],
        |    "Providers": [],
        |    "ExtraItems": null,
        |    "ExtraExtensions": []
        |    }
        |  ]
        |}
        """.trimMargin()

        val bundle = EpicAppointmentBundle(json)
        assertEquals(json, bundle.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, bundle.dataSource)
        assertEquals(0, bundle.links.size)

        val appointment1 = bundle.resources[0]
        assertEquals(visitIdentifierJson1, appointment1.identifier[0].raw)
        assertEquals("25000", appointment1.identifier[0].value)
        assertEquals("Internal", appointment1.identifier[0].system)
        assertEquals("2015-04-30T15:30:00", appointment1.start)
        assertNull(appointment1.appointmentType)
        assertEquals("TRANSPLANT EVALUATION", appointment1.serviceType[0].text)
        assertEquals(AppointmentStatus.NOSHOW, appointment1.status)

        val appointment2 = bundle.resources[1]
        assertEquals(visitIdentifierJson2, appointment2.identifier[0].raw)
        assertEquals("25001", appointment2.identifier[0].value)
        assertEquals("Internal", appointment2.identifier[0].system)
        assertEquals("2015-04-29T11:45:00", appointment2.start)
        assertNull(appointment2.appointmentType)
        assertEquals("INFUSION 120", appointment2.serviceType[0].text)
        assertEquals(AppointmentStatus.FULFILLED, appointment2.status)
    }

    @Test
    fun `ensure no appointments handled`() {
        val json = """|{
        |  "Error": null
        |}""".trimMargin()
        val bundle = EpicAppointmentBundle(json)
        assertEquals(json, bundle.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, bundle.dataSource)
        assertEquals(listOf<EpicAppointment>(), bundle.resources)
    }
}
