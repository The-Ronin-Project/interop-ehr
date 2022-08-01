package com.projectronin.interop.ehr.epic.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.model.ReferenceTypes
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

        val epicAppointment = EpicAppointment(appointment, emptyMap(), null)
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals(visitIdentifier.id, epicAppointment.identifier[0].value)
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

        val epicAppointment = EpicAppointment(appointment, emptyMap(), null)
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals(visitIdentifier.id, epicAppointment.identifier[0].value)
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

        val epicAppointment = EpicAppointment(appointment, emptyMap(), null)
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals(visitIdentifier.id, epicAppointment.identifier[0].value)
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
        |  "PatientIDs" : [],
        |  "PatientName": "LMRTESTING,HERMIONE",
        |  "Providers": [],
        |  "VisitTypeIDs": [
        |    $visitIdentifierJson
        |  ],
        |  "VisitTypeName": "TRANSPLANT EVALUATION"
        |}
        """.trimMargin()

        val epicAppointment = EpicAppointment(appointment, emptyMap(), null)
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(deformat(json), epicAppointment.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals(visitIdentifier.id, epicAppointment.identifier[0].value)
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
            duration = "",
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
        |  "PatientIDs":[],
        |  "PatientName": "LMRTESTING,HERMIONE",
        |  "Providers":[{"DepartmentIDs":[],"DepartmentName":"Blank","Duration":"","ProviderIDs":[{"ID":"2100","Type":"Internal"}],"ProviderName":"Davey","Time":"900"}],
        |  "VisitTypeIDs": [
        |    $visitIdentifierJson
        |  ],
        |  "VisitTypeName": "TRANSPLANT EVALUATION"
        |}
        """.trimMargin()

        val epicAppointment = EpicAppointment(appointment, emptyMap(), null)
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(deformat(json), epicAppointment.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals("25000", epicAppointment.identifier[0].value)
        assertEquals("Internal", epicAppointment.identifier[0].type?.text)
        assertEquals(null, epicAppointment.identifier[0].system)
        assertEquals("2015-12-03T15:30:00", epicAppointment.start)
        assertEquals("TRANSPLANT EVALUATION", epicAppointment.serviceType[0].text)
        assertEquals(2, epicAppointment.participants.size)
        assertEquals(provider.providerName, epicAppointment.participants.first().actor.display)
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
        |  "PatientIDs" : [],
        |  "PatientName": "LMRTESTING,HERMIONE",
        |  "Providers":[{"DepartmentIDs":[],"DepartmentName":"Blank","Duration":"30","ProviderIDs":[{"ID":"2100","Type":"Internal"}],"ProviderName":"Davey","Time":"900"}],
        |  "VisitTypeIDs": [
        |    $visitIdentifierJson
        |  ],
        |  "VisitTypeName": "TRANSPLANT EVALUATION"
        |}
        """.trimMargin()
        val epicAppointment = EpicAppointment(appointment, emptyMap(), null)
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(deformat(json), epicAppointment.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals("25000", epicAppointment.identifier[0].value)
        assertEquals("Internal", epicAppointment.identifier[0].type?.text)
        assertEquals(null, epicAppointment.identifier[0].system)
        assertEquals("2015-12-03T15:30:00", epicAppointment.start)
        assertEquals("TRANSPLANT EVALUATION", epicAppointment.serviceType[0].text)
        assertEquals(2, epicAppointment.participants.size)
        assertEquals(provider.providerName, epicAppointment.participants.first().actor.display)
        assertEquals(AppointmentStatus.NOSHOW, epicAppointment.status)
    }

    @Test
    fun `patient participants`() {
        val visitIdentifier = IDType(id = "25000", type = "Internal")
        val providerIdentifier = IDType(id = "2100", type = "Internal")
        val patientIdentifier = IDType(id = "paitientId", type = "External").toIdentifier()
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
        |  "PatientIDs":[],
        |  "PatientName": "LMRTESTING,HERMIONE",
        |  "Providers":[{"DepartmentIDs":[], "DepartmentName":"Blank","Duration":"30","ProviderIDs":[{"ID":"2100","Type":"Internal"}],"ProviderName":"Davey","Time":"900"}],
        |  "VisitTypeIDs": [
        |    $visitIdentifierJson
        |  ],
        |  "VisitTypeName": "TRANSPLANT EVALUATION"
        |}
        """.trimMargin()
        val epicAppointment = EpicAppointment(appointment, emptyMap(), patientIdentifier)
        assertEquals(appointment, epicAppointment.resource)
        assertEquals(deformat(json), epicAppointment.raw)
        assertEquals(DataSource.EPIC_APPORCHARD, epicAppointment.dataSource)
        assertEquals(ResourceType.APPOINTMENT, epicAppointment.resourceType)
        assertEquals("22792", epicAppointment.id)
        assertEquals("25000", epicAppointment.identifier[0].value)
        assertEquals("Internal", epicAppointment.identifier[0].type?.text)
        assertEquals(null, epicAppointment.identifier[0].system)
        assertEquals("2015-12-03T15:30:00", epicAppointment.start)
        assertEquals("TRANSPLANT EVALUATION", epicAppointment.serviceType[0].text)
        assertEquals(2, epicAppointment.participants.size)
        assertEquals(
            provider.providerName,
            epicAppointment.participants.first { it.actor.type == "Practitioner" }.actor.display
        )
        assertEquals(
            appointment.patientName,
            epicAppointment.participants.first { it.actor.type == "Patient" }.actor.display
        )
        assertEquals(AppointmentStatus.NOSHOW, epicAppointment.status)
    }

    @Test
    fun `can serialize`() {
        val visitIdentifier = IDType(id = "25000", type = "Internal")
        val providerIdentifier = IDType(id = "2100", type = "Internal")
        val patientIdentifier = IDType(id = "patientId", type = "External")
        val patientIDTypeEpic = Identifier(
            value = "patientId",
            type = CodeableConcept(text = "External"),
            system = Uri("systemFromIdentifierService")
        )
        val providerIDTypeEpic = Identifier(
            value = "2100",
            type = CodeableConcept(text = "Internal"),
            system = Uri("systemFromIdentifierService")
        )
        val provider = ScheduleProviderReturnWithTime(
            departmentIDs = listOf(),
            departmentName = "Blank",
            providerIDs = listOf(providerIdentifier),
            duration = "",
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
            extraExtensions = listOf(),
            patientIDs = listOf(patientIdentifier)
        )

        val epicAppointment = EpicAppointment(
            appointment,
            mapOf(
                provider to providerIDTypeEpic
            ),
            patientIDTypeEpic
        )
        val serialized =
            JacksonManager.nonAbsentObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(epicAppointment)
        KotlinLogging.logger { }.info { serialized }

        val expected = this::class.java.getResource("/ExampleSerializedEpicAppointment.json")!!.readText()
        assertEquals(expected, serialized)
    }

    @Test
    fun `can serialize with empty providerIDs`() {
        val visitIdentifier = IDType(id = "25000", type = "Internal")
        val providerIdentifier = IDType(id = "2100", type = "Internal")
        val patientIdentifier = IDType(id = "paitientId", type = "External")
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
            extraExtensions = listOf(),
            patientIDs = listOf(patientIdentifier)
        )

        val epicAppointment = EpicAppointment(
            appointment,
            null,
            null,
        )
        val serialized = JacksonManager.objectMapper.copy().setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
            .writerWithDefaultPrettyPrinter().writeValueAsString(epicAppointment)
        val expected = this::class.java.getResource("/ExampleSerializedEpicAppointmentWithNulls.json")!!.readText()
        assertEquals(expected, serialized)
    }

    @Test
    fun `can deserialize`() {
        val json = this::class.java.getResource("/ExampleSerializedEpicAppointment.json")!!.readText()
        val deserializedAppt = JacksonManager.objectMapper.readValue(json, EpicAppointment::class.java)
        assertEquals("22792", deserializedAppt.id)
        assertEquals(2, deserializedAppt.participants.size)
        val patients = deserializedAppt.participants.filter { it.actor.type == ReferenceTypes.PATIENT }
        assertEquals(1, patients.size)
        val patientIdentifier = patients.single().actor.identifier
        assertEquals("patientId", patientIdentifier?.value)

        val providers = deserializedAppt.participants.filter { it.actor.type == ReferenceTypes.PRACTITIONER }
        assertEquals(1, providers.size)
        val providerIdentifier = providers.single().actor.identifier
        assertEquals("2100", providerIdentifier?.value)
        assertEquals("systemFromIdentifierService", providerIdentifier?.system?.value)
    }

    @Test
    fun `can deserialize with nulls`() {
        val json = this::class.java.getResource("/ExampleSerializedEpicAppointmentWithNulls.json")!!.readText()
        val deserializedAppt = JacksonManager.objectMapper.readValue(json, EpicAppointment::class.java)
        assertEquals("22792", deserializedAppt.id)
        assertEquals(2, deserializedAppt.participants.size)
        val patients = deserializedAppt.participants.filter { it.actor.type == ReferenceTypes.PATIENT }
        assertEquals(1, patients.size)
        val patientIdentifier = patients.single().actor.identifier
        assertNull(patientIdentifier)

        val providers = deserializedAppt.participants.filter { it.actor.type == ReferenceTypes.PRACTITIONER }
        assertEquals(1, providers.size)
        val providerIdentifier = providers.single().actor.identifier
        assertNull(providerIdentifier)
    }
}
