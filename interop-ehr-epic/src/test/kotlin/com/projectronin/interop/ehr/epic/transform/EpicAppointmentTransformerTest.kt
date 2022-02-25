package com.projectronin.interop.ehr.epic.transform

import com.projectronin.interop.ehr.epic.apporchard.model.ExtensionInformationReturn
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ItemValue
import com.projectronin.interop.ehr.epic.apporchard.model.LineValue
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.epic.apporchard.model.SubLine
import com.projectronin.interop.ehr.epic.readResource
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.ExtensionMeanings
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment as AOAppointment

class EpicAppointmentTransformerTest {
    private val transformer = EpicAppointmentTransformer()

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val epicAppointment = readResource<AOAppointment>("/ExampleEpicAppointment.json")

    @Test
    fun `transforms appointment with all attributes`() {
        val aoAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "scheduled",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            extraExtensions = listOf(
                ExtensionInformationReturn(
                    extensionIds = listOf(
                        IDType(
                            id = "abc",
                            type = "type"
                        )
                    ),
                    extensionName = "extension name",
                    lines = listOf(
                        LineValue(
                            lineNumber = 1,
                            subLines = listOf(SubLine(subLineNumber = 2, value = "subline value")),
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
                            subLines = listOf(SubLine(subLineNumber = 2, value = "subline value")),
                            value = "line value"
                        )
                    ),
                    value = "item value"
                )
            ),
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerIDs = listOf(IDType(id = "9876", type = "Internal")),
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)

        oncologyAppointment!!
        assertEquals(Id(value = "test-12345"), oncologyAppointment.id)
        assertEquals(
            listOf(
                Extension(
                    url = ExtensionMeanings.PARTNER_DEPARTMENT.uri,
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Organization/test-6789"))
                )
            ),
            oncologyAppointment.extension
        )
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_ID, system = CodeSystem.RONIN_ID.uri, value = "test-12345"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyAppointment.identifier
        )
        assertEquals(AppointmentStatus.PENDING, oncologyAppointment.status)
        assertEquals(
            CodeableConcept(text = "Test visit type"),
            oncologyAppointment.appointmentType
        )
        assertEquals(Instant("2015-04-30T20:30:00Z"), oncologyAppointment.start)
        assertEquals(Instant("2015-04-30T21:00:00Z"), oncologyAppointment.end)
        assertEquals(30, oncologyAppointment.minutesDuration)
        assertEquals("Notes", oncologyAppointment.comment)
        assertEquals(
            listOf(
                Participant(
                    actor =
                    Reference(
                        reference = "Patient/test-54321",
                        display = "Test Name"
                    ),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor =
                    Reference(
                        reference = "Practitioner/test-9876",
                        display = "Test Doc"
                    ),
                    status = ParticipationStatus.ACCEPTED,
                    period = Period(
                        start = DateTime(value = "2015-04-30T20:30:00Z"),
                        end = DateTime(value = "2015-04-30T20:45:00Z")
                    )
                )
            ),
            oncologyAppointment.participant
        )
    }

    @Test
    fun `transform appointment with only required attributes`() {
        val aoAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "completed",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)
        oncologyAppointment!!

        assertEquals("Appointment", oncologyAppointment.resourceType)
        assertEquals(Id(value = "test-12345"), oncologyAppointment.id)
        assertEquals(
            listOf(
                Extension(
                    url = ExtensionMeanings.PARTNER_DEPARTMENT.uri,
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Organization/test-6789"))
                )
            ),
            oncologyAppointment.extension
        )
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_ID, system = CodeSystem.RONIN_ID.uri, value = "test-12345"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyAppointment.identifier
        )
        assertEquals(AppointmentStatus.FULFILLED, oncologyAppointment.status)
        assertEquals(
            CodeableConcept(text = "Test visit type"),
            oncologyAppointment.appointmentType
        )
        assertEquals(Instant("2015-04-30T20:30:00Z"), oncologyAppointment.start)
        assertEquals(Instant("2015-04-30T21:00:00Z"), oncologyAppointment.end)
        assertEquals(30, oncologyAppointment.minutesDuration)
        assertNull(oncologyAppointment.comment)
        assertEquals(
            listOf(
                Participant(
                    actor =
                    Reference(
                        reference = "Patient/test-54321",
                        display = "Test Name"
                    ),
                    status = ParticipationStatus.ACCEPTED
                )
            ),
            oncologyAppointment.participant
        )
        assertNull(oncologyAppointment.meta)
        assertNull(oncologyAppointment.implicitRules)
        assertNull(oncologyAppointment.language)
        assertNull(oncologyAppointment.text)
        assertEquals(listOf<ContainedResource>(), oncologyAppointment.contained)
        assertEquals(listOf<Extension>(), oncologyAppointment.modifierExtension)
        assertNull(oncologyAppointment.cancellationReason)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.serviceCategory)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.serviceType)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.specialty)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.reasonCode)
        assertEquals(listOf<Reference>(), oncologyAppointment.reasonReference)
        assertNull(oncologyAppointment.priority)
        assertNull(oncologyAppointment.description)
        assertEquals(listOf<Reference>(), oncologyAppointment.supportingInformation)
        assertEquals(listOf<Reference>(), oncologyAppointment.slot)
        assertNull(oncologyAppointment.created)
        assertNull(oncologyAppointment.patientInstruction)
        assertEquals(listOf<Reference>(), oncologyAppointment.basedOn)
        assertEquals(listOf<Period>(), oncologyAppointment.requestedPeriod)
    }

    @Test
    fun `transforms real AppOrchard data`() {
        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns epicAppointment
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)

        oncologyAppointment!!
        assertEquals(Id(value = "test-22792"), oncologyAppointment.id)
        assertEquals(
            listOf(
                Extension(
                    url = ExtensionMeanings.PARTNER_DEPARTMENT.uri,
                    value = DynamicValue(
                        DynamicValueType.REFERENCE,
                        Reference(reference = "Organization/test-10501205")
                    )
                )
            ),
            oncologyAppointment.extension
        )
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_ID, system = CodeSystem.RONIN_ID.uri, value = "test-22792"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyAppointment.identifier
        )
        assertEquals(AppointmentStatus.NOSHOW, oncologyAppointment.status)
        assertEquals(
            CodeableConcept(text = "TRANSPLANT EVALUATION"),
            oncologyAppointment.appointmentType
        )
        assertEquals(Instant("2015-04-30T20:30:00Z"), oncologyAppointment.start)
        assertEquals(Instant("2015-04-30T21:00:00Z"), oncologyAppointment.end)
        assertEquals(30, oncologyAppointment.minutesDuration)
        assertNull(oncologyAppointment.comment)
        assertEquals(
            listOf(
                Participant(
                    actor =
                    Reference(
                        reference = "Patient/test-Z5660",
                        display = "LMRTESTING,HERMIONE"
                    ),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor =
                    Reference(
                        reference = "Practitioner/test-E400019",
                        display = "Coordinator Phoenix, RN"
                    ),
                    status = ParticipationStatus.ACCEPTED,
                    period = Period(
                        start = DateTime(value = "2015-04-30T20:30:00Z"),
                        end = DateTime(value = "2015-04-30T20:45:00Z")
                    )
                )
            ),
            oncologyAppointment.participant
        )
    }

    @Test
    fun `transform appointments with all appointment statuses`() {
        val completedAOAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "completed",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val completedAppointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns completedAOAppointment
        }

        val completedOncologyAppointment = transformer.transformAppointment(completedAppointment, tenant)!!
        assertEquals(AppointmentStatus.FULFILLED, completedOncologyAppointment.status)

        val scheduledAOAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "scheduled",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val scheduledAppointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns scheduledAOAppointment
        }

        val scheduledOncologyAppointment = transformer.transformAppointment(scheduledAppointment, tenant)!!
        assertEquals(AppointmentStatus.PENDING, scheduledOncologyAppointment.status)

        val noShowAOAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "no show",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val noShowAppointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns noShowAOAppointment
        }

        val noShowOncologyAppointment = transformer.transformAppointment(noShowAppointment, tenant)!!
        assertEquals(AppointmentStatus.NOSHOW, noShowOncologyAppointment.status)

        val arrivedAOAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "arrived",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val arrivedAppointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns arrivedAOAppointment
        }

        val arrivedOncologyAppointment = transformer.transformAppointment(arrivedAppointment, tenant)!!
        assertEquals(AppointmentStatus.ARRIVED, arrivedOncologyAppointment.status)

        val unknownAOAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "unknown",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val unknownAppointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns unknownAOAppointment
        }

        val unknownOncologyAppointment = transformer.transformAppointment(unknownAppointment, tenant)!!
        assertEquals(AppointmentStatus.ENTERED_IN_ERROR, unknownOncologyAppointment.status)
    }

    @Test
    fun `non AppOrchard appointment`() {
        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformAppointment(appointment, tenant)
        }

        assertEquals("Appointment is not an Epic AppOrchard resource", exception.message)
    }

    @Test
    fun `fails for appointment with missing id`() {
        val aoAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "completed",
            contactIDs = listOf(IDType(id = "12345", type = "not ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on missing partnerReference extension`() {
        val aoAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "completed",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Bad Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on no participants`() {
        val aoAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "completed",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "BAD Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "BAD Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `non R4 bundle`() {
        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.FHIR_R4
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformAppointments(bundle, tenant)
        }

        assertEquals("Bundle is not an Epic AppOrchard resource", exception.message)
    }

    @Test
    fun `bundle transformation returns empty when no valid transformations`() {
        val invalidAOAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "completed",
            contactIDs = listOf(IDType(id = "12345", type = "not ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )
        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns invalidAOAppointment
        }

        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns invalidAOAppointment
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(0, oncologyAppointments.size)
    }

    @Test
    fun `bundle transformation returns only valid transformations`() {
        val invalidAOAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "completed",
            contactIDs = listOf(IDType(id = "12345", type = "not ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )
        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns invalidAOAppointment
        }

        val aoAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "completed",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )
        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(1, oncologyAppointments.size)
    }

    @Test
    fun `bundle transformation returns all when all valid`() {
        val aoAppointment = AOAppointment(
            appointmentDuration = "30",
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "completed",
            contactIDs = listOf(IDType(id = "12345", type = "ASN")),
            date = "4/30/2015",
            patientIDs = listOf(IDType(id = "54321", type = "Internal")),
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "6789",
                            type = "Internal"
                        )
                    ),
                    departmentName = "Test department",
                    duration = "15",
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeIDs = listOf(IDType(id = "abcd", type = "Internal")),
            visitTypeName = "Test visit type"
        )

        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
        }

        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(2, oncologyAppointments.size)
    }
}
