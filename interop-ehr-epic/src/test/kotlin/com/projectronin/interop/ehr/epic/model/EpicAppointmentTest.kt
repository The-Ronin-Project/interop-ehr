package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicAppointmentTest {
    @Test
    fun `can build from object`() {
        val visitIdentifier = IDType(id = "25000", type = "Internal")
        val appointment = Appointment(
            patientName = "LMRTESTING,HERMIONE",
            date = "12/3/2015",
            visitTypeName = "TRANSPLANT EVALUATION",
            appointmentNotes = listOf(),
            appointmentStartTime = " 3:30 PM",
            appointmentDuration = "30",
            appointmentStatus = "No Show",
            contactIDs = listOf(IDType("22792", "CSN"), IDType("22792", "ASN")),
            visitTypeIDs = listOf(visitIdentifier),
            providers = listOf(),
            extraItems = null,
            extraExtensions = listOf()
        )

        val epicAppointment = EpicAppointment(appointment, emptyMap())
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals(visitIdentifier, epicAppointment.identifier[0].element)
        assertEquals("25000", epicAppointment.identifier[0].value)
        assertEquals("Internal", epicAppointment.identifier[0].type?.text)
        assertEquals(null, epicAppointment.identifier[0].system)
        assertEquals("2015-12-03T15:30:00", epicAppointment.start)
        assertEquals("TRANSPLANT EVALUATION", epicAppointment.serviceType[0].text)
        assertEquals(AppointmentStatus.NOSHOW, epicAppointment.status)
    }

    @Test
    fun `handles 2 digit days`() {
        val visitIdentifier = IDType(id = "25000", type = "Internal")
        val appointment = Appointment(
            patientName = "LMRTESTING,HERMIONE",
            date = "12/13/2015",
            visitTypeName = "TRANSPLANT EVALUATION",
            appointmentNotes = listOf(),
            appointmentStartTime = " 3:30 PM",
            appointmentDuration = "30",
            appointmentStatus = "No Show",
            contactIDs = listOf(IDType("22792", "CSN"), IDType("22792", "ASN")),
            visitTypeIDs = listOf(visitIdentifier),
            providers = listOf(),
            extraItems = null,
            extraExtensions = listOf()
        )

        val epicAppointment = EpicAppointment(appointment, emptyMap())
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals(visitIdentifier, epicAppointment.identifier[0].element)
        assertEquals("25000", epicAppointment.identifier[0].value)
        assertEquals("Internal", epicAppointment.identifier[0].type?.text)
        assertEquals(null, epicAppointment.identifier[0].system)
        assertEquals("2015-12-13T15:30:00", epicAppointment.start)
        assertEquals("TRANSPLANT EVALUATION", epicAppointment.serviceType[0].text)
        assertEquals(AppointmentStatus.NOSHOW, epicAppointment.status)
    }

    @Test
    fun `handles 1 digit months`() {
        val visitIdentifier = IDType(id = "25000", type = "Internal")
        val appointment = Appointment(
            patientName = "LMRTESTING,HERMIONE",
            date = "1/13/2015",
            visitTypeName = "TRANSPLANT EVALUATION",
            appointmentNotes = listOf(),
            appointmentStartTime = " 3:30 PM",
            appointmentDuration = "30",
            appointmentStatus = "No Show",
            contactIDs = listOf(IDType("22792", "CSN"), IDType("22792", "ASN")),
            visitTypeIDs = listOf(visitIdentifier),
            providers = listOf(),
            extraItems = null,
            extraExtensions = listOf()
        )

        val epicAppointment = EpicAppointment(appointment, emptyMap())
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals(visitIdentifier, epicAppointment.identifier[0].element)
        assertEquals("25000", epicAppointment.identifier[0].value)
        assertEquals("Internal", epicAppointment.identifier[0].type?.text)
        assertEquals(null, epicAppointment.identifier[0].system)
        assertEquals("2015-01-13T15:30:00", epicAppointment.start)
        assertEquals("TRANSPLANT EVALUATION", epicAppointment.serviceType[0].text)
        assertEquals(AppointmentStatus.NOSHOW, epicAppointment.status)
    }

    @Test
    fun `returns JSON as raw`() {
        val visitIdentifier = IDType(id = "25000", type = "Internal")
        val appointment = Appointment(
            patientName = "LMRTESTING,HERMIONE",
            date = "12/3/2015",
            visitTypeName = "TRANSPLANT EVALUATION",
            appointmentNotes = listOf(),
            appointmentStartTime = " 3:30 PM",
            appointmentDuration = "30",
            appointmentStatus = "No Show",
            contactIDs = listOf(IDType("22792", "CSN"), IDType("22792", "ASN")),
            visitTypeIDs = listOf(visitIdentifier),
            providers = listOf(),
            extraItems = null,
            extraExtensions = listOf()
        )
        val visitIdentifierJson = """{"ID":"25000","Type":"Internal"}"""

        val json = """{
        |  "AppointmentDuration": "30",
        |  "AppointmentNotes": [],
        |  "AppointmentStartTime": " 3:30 PM",
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
        |  "Date": "12/3/2015",
        |  "ExtraExtensions": [],
        |  "ExtraItems": null,
        |  "PatientName": "LMRTESTING,HERMIONE",
        |  "Providers": [],
        |  "VisitTypeIDs": [
        |    $visitIdentifierJson
        |  ],
        |  "VisitTypeName": "TRANSPLANT EVALUATION"
        |}
        """.trimMargin()

        val epicAppointment = EpicAppointment(appointment, emptyMap())
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(deformat(json), epicAppointment.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals(visitIdentifier, epicAppointment.identifier[0].element)
        assertEquals(visitIdentifierJson, epicAppointment.identifier[0].raw)
        assertEquals("25000", epicAppointment.identifier[0].value)
        assertEquals("Internal", epicAppointment.identifier[0].type?.text)
        assertEquals(null, epicAppointment.identifier[0].system)
        assertEquals("2015-12-03T15:30:00", epicAppointment.start)
        assertEquals("TRANSPLANT EVALUATION", epicAppointment.serviceType[0].text)
        assertEquals(AppointmentStatus.NOSHOW, epicAppointment.status)
    }

    @Test
    fun `providers are correct`() {
        val visitIdentifier = IDType(id = "25000", type = "Internal")
        val providerIdentifier = IDType(id = "2100", type = "Internal")
        val provider = ScheduleProviderReturnWithTime(
            departmentIDs = listOf(),
            departmentName = "Blank",
            providerIDs = listOf(providerIdentifier),
            duration = "30",
            providerName = "Davey",
            time = "900"
        )
        val appointment = Appointment(
            patientName = "LMRTESTING,HERMIONE",
            date = "12/3/2015",
            visitTypeName = "TRANSPLANT EVALUATION",
            appointmentNotes = listOf(),
            appointmentStartTime = " 3:30 PM",
            appointmentDuration = "30",
            appointmentStatus = "No Show",
            contactIDs = listOf(IDType("22792", "CSN"), IDType("22792", "ASN")),
            visitTypeIDs = listOf(visitIdentifier),
            providers = listOf(provider),
            extraItems = null,
            extraExtensions = listOf()
        )
        val visitIdentifierJson = """{"ID":"25000","Type":"Internal"}"""

        val json = """{
        |  "AppointmentDuration": "30",
        |  "AppointmentNotes": [],
        |  "AppointmentStartTime": " 3:30 PM",
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
        |  "Date": "12/3/2015",
        |  "ExtraExtensions": [],
        |  "ExtraItems": null,
        |  "PatientName": "LMRTESTING,HERMIONE",
        |  "Providers":[{"DepartmentName":"Blank","Duration":"30","ProviderIDs":[{"ID":"2100","Type":"Internal"}],"ProviderName":"Davey","Time":"900"}],
        |  "VisitTypeIDs": [
        |    $visitIdentifierJson
        |  ],
        |  "VisitTypeName": "TRANSPLANT EVALUATION"
        |}
        """.trimMargin()

        val epicAppointment = EpicAppointment(appointment, emptyMap())
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(deformat(json), epicAppointment.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals(visitIdentifier, epicAppointment.identifier[0].element)
        assertEquals(visitIdentifierJson, epicAppointment.identifier[0].raw)
        assertEquals("25000", epicAppointment.identifier[0].value)
        assertEquals("Internal", epicAppointment.identifier[0].type?.text)
        assertEquals(null, epicAppointment.identifier[0].system)
        assertEquals("2015-12-03T15:30:00", epicAppointment.start)
        assertEquals("TRANSPLANT EVALUATION", epicAppointment.serviceType[0].text)
        assertEquals(1, epicAppointment.participants.size)
        assertEquals(provider.providerName, epicAppointment.participants.first().actor.first().display)
        assertEquals(AppointmentStatus.NOSHOW, epicAppointment.status)
    }

    @Test
    fun `providers with no identifiers work`() {
        val visitIdentifier = IDType(id = "25000", type = "Internal")
        val providerIdentifier = IDType(id = "2100", type = "Internal")
        val provider = ScheduleProviderReturnWithTime(
            departmentIDs = listOf(),
            departmentName = "Blank",
            providerIDs = listOf(providerIdentifier),
            duration = "30",
            providerName = "Davey",
            time = "900"
        )
        val appointment = Appointment(
            patientName = "LMRTESTING,HERMIONE",
            date = "12/3/2015",
            visitTypeName = "TRANSPLANT EVALUATION",
            appointmentNotes = listOf(),
            appointmentStartTime = " 3:30 PM",
            appointmentDuration = "30",
            appointmentStatus = "No Show",
            contactIDs = listOf(IDType("22792", "CSN"), IDType("22792", "ASN")),
            visitTypeIDs = listOf(visitIdentifier),
            providers = listOf(provider),
            extraItems = null,
            extraExtensions = listOf()
        )
        val visitIdentifierJson = """{"ID":"25000","Type":"Internal"}"""

        val json = """{
        |  "AppointmentDuration": "30",
        |  "AppointmentNotes": [],
        |  "AppointmentStartTime": " 3:30 PM",
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
        |  "Date": "12/3/2015",
        |  "ExtraExtensions": [],
        |  "ExtraItems": null,
        |  "PatientName": "LMRTESTING,HERMIONE",
        |  "Providers":[{"DepartmentName":"Blank","Duration":"30","ProviderIDs":[{"ID":"2100","Type":"Internal"}],"ProviderName":"Davey","Time":"900"}],
        |  "VisitTypeIDs": [
        |    $visitIdentifierJson
        |  ],
        |  "VisitTypeName": "TRANSPLANT EVALUATION"
        |}
        """.trimMargin()
        val epicAppointment = EpicAppointment(appointment, mapOf(appointment.id to emptyMap()))
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(deformat(json), epicAppointment.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals(visitIdentifier, epicAppointment.identifier[0].element)
        assertEquals(visitIdentifierJson, epicAppointment.identifier[0].raw)
        assertEquals("25000", epicAppointment.identifier[0].value)
        assertEquals("Internal", epicAppointment.identifier[0].type?.text)
        assertEquals(null, epicAppointment.identifier[0].system)
        assertEquals("2015-12-03T15:30:00", epicAppointment.start)
        assertEquals("TRANSPLANT EVALUATION", epicAppointment.serviceType[0].text)
        assertEquals(1, epicAppointment.participants.size)
        assertEquals(provider.providerName, epicAppointment.participants.first().actor.first().display)
        assertEquals(AppointmentStatus.NOSHOW, epicAppointment.status)
    }
}
