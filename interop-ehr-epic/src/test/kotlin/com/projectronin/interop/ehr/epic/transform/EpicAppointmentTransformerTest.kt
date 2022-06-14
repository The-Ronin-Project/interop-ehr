package com.projectronin.interop.ehr.epic.transform

import com.projectronin.interop.aidbox.PatientService
import com.projectronin.interop.aidbox.PractitionerService
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.ehr.epic.apporchard.model.ExtensionInformationReturn
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ItemValue
import com.projectronin.interop.ehr.epic.apporchard.model.LineValue
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.epic.apporchard.model.SubLine
import com.projectronin.interop.ehr.epic.model.EpicAppointment
import com.projectronin.interop.ehr.epic.model.EpicIDType
import com.projectronin.interop.ehr.epic.model.EpicPatientParticipant
import com.projectronin.interop.ehr.epic.model.EpicPatientReference
import com.projectronin.interop.ehr.epic.model.EpicProviderParticipant
import com.projectronin.interop.ehr.epic.model.EpicProviderReference
import com.projectronin.interop.ehr.epic.readResource
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.ReferenceTypes
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
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment as AOAppointment
import com.projectronin.interop.ehr.model.Identifier as EHRIdentifier
import com.projectronin.interop.ehr.model.Participant as EHRParticipant
import com.projectronin.interop.ehr.model.Reference as EHRReference

class EpicAppointmentTransformerTest {
    private lateinit var mockPractitionerService: PractitionerService
    private lateinit var mockPatientService: PatientService
    private lateinit var transformer: EpicAppointmentTransformer
    private lateinit var aoAppointment: AOAppointment

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val epicAppointment = readResource<AOAppointment>("/ExampleEpicAppointment.json")
    private val patientParticipant = EpicPatientParticipant(
        element = "Patient Name", identifier = EpicIDType(IDType("patientEpicId", "External"))
    )
    private val mockProviderIdentifier = mockk<EHRIdentifier> {
        every { value } returns "providerId"
        every { type?.text } returns "External"
        every { system } returns ""
    }
    private val mockProviderReference = mockk<EpicProviderReference> {
        every { display } returns "Coordinator Phoenix, RN"
        every { identifier } returns mockProviderIdentifier
        every { type } returns "Practitioner"
    }
    private val providerParticipant = mockk<EpicProviderParticipant> {
        every { actor } returns mockProviderReference
    }
    private val mockParticipants = listOf<EHRParticipant>(patientParticipant, providerParticipant)

    @BeforeEach
    fun setup() {
        mockPractitionerService = mockk {
            every {
                getPractitionerFHIRIds(
                    "test",
                    mapOf(mockProviderIdentifier to SystemValue("providerId", ""))
                )
            } returns emptyMap()
        }
        mockPatientService = mockk {
            every {
                getPatientFHIRIds("test", mapOf("any" to SystemValue("patientEpicId", "")))
            } returns mapOf("any" to "test-patientFhirId")
        }

        transformer = EpicAppointmentTransformer(mockPractitionerService, mockPatientService)
        aoAppointment = AOAppointment(
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
    }

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

        val appointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns mockParticipants
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)

        assertNotNull(oncologyAppointment)
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
                        reference = "Patient/${tenant.mnemonic}-patientFhirId",
                        display = "Patient Name",
                        type = Uri("Patient")
                    ),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor =
                    Reference(
                        identifier = Identifier(value = "providerId", type = CodeableConcept(text = "External")),
                        display = "Coordinator Phoenix, RN",
                        type = Uri("Practitioner")
                    ),
                    status = ParticipationStatus.ACCEPTED
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

        val appointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns mockParticipants
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)
        assertNotNull(oncologyAppointment)
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
                        reference = "Patient/${tenant.mnemonic}-patientFhirId",
                        display = "Patient Name",
                        type = Uri("Patient")
                    ),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor =
                    Reference(
                        identifier = Identifier(value = "providerId", type = CodeableConcept(text = "External")),
                        display = "Coordinator Phoenix, RN",
                        type = Uri("Practitioner")
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
        assertNull(oncologyAppointment.cancelationReason)
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
        val appointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns epicAppointment
            every { participants } returns mockParticipants
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)
        assertNotNull(oncologyAppointment)
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
                        reference = "Patient/${tenant.mnemonic}-patientFhirId",
                        display = "Patient Name",
                        type = Uri("Patient")
                    ),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor =
                    Reference(
                        identifier = Identifier(value = "providerId", type = CodeableConcept(text = "External")),
                        display = "Coordinator Phoenix, RN",
                        type = Uri("Practitioner")
                    ),
                    status = ParticipationStatus.ACCEPTED
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

        val completedAppointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns completedAOAppointment
            every { participants } returns mockParticipants
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

        val scheduledAppointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns scheduledAOAppointment
            every { participants } returns mockParticipants
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

        val noShowAppointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns noShowAOAppointment
            every { participants } returns mockParticipants
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

        val arrivedAppointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns arrivedAOAppointment
            every { participants } returns mockParticipants
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

        val unknownAppointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns unknownAOAppointment
            every { participants } returns mockParticipants
        }

        val unknownOncologyAppointment = transformer.transformAppointment(unknownAppointment, tenant)!!
        assertEquals(AppointmentStatus.ENTERED_IN_ERROR, unknownOncologyAppointment.status)
    }

    @Test
    fun `non AppOrchard appointment`() {
        val appointment = mockk<EpicAppointment>() {
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

        val appointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns mockParticipants
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on missing partnerReference extension`() {
        val appointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns emptyList()
        }
        every {
            mockPractitionerService.getPractitionerFHIRIds(
                "test",
                emptyMap<Identifier, SystemValue>()
            )
        } returns emptyMap()

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails on no participants`() {
        val appointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns emptyList()
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
        val appointment1 = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns invalidAOAppointment
            every { participants } returns mockParticipants
        }

        val appointment2 = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns invalidAOAppointment
            every { participants } returns mockParticipants
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
        val appointment1 = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns invalidAOAppointment
            every { participants } returns mockParticipants
        }
        val appointment2 = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns mockParticipants
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
        val appointment1 = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns mockParticipants
        }

        val appointment2 = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns mockParticipants
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(2, oncologyAppointments.size)
    }

    @Test
    fun `null participant checks`() {
        val mockIdentifier = mockk<EHRIdentifier> {
            every { value } returns "providerId"
            every { type } returns null
        }
        val mockProviderReference1 = mockk<EHRReference> {
            every { display } returns "Unknown"
            every { identifier } returns mockIdentifier
            every { type } returns null
        }
        val mockUnknownParticipant = mockk<EHRParticipant> {
            every { actor } returns mockProviderReference1
        }

        val mockProviderReference2 = mockk<EpicProviderReference> {
            every { display } returns "Coordinator Phoenix, RN"
            every { identifier } returns null
            every { type } returns "Practitioner"
        }

        val providerParticipant = mockk<EpicProviderParticipant> {
            every { actor } returns mockProviderReference2
        }
        val mockParticipants = listOf(patientParticipant, mockUnknownParticipant, providerParticipant)

        val appointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns mockParticipants
        }
        every {
            mockPractitionerService.getPractitionerFHIRIds(
                "test",
                mapOf(null to SystemValue("", ""))
            )
        } returns emptyMap()

        val appt = transformer.transformAppointment(appointment, tenant)
        assertNotNull(appt)
    }

    @Test
    fun `transform appointment provided fhir references`() {
        val appointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns mockParticipants
        }
        every {
            mockPractitionerService.getPractitionerFHIRIds(
                "test",
                mapOf(mockProviderIdentifier to SystemValue("providerId", ""))
            )
        } returns mapOf(mockProviderIdentifier to "test-practitionerFhirId")
        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)
        assertNotNull(oncologyAppointment)
        oncologyAppointment!!

        assertEquals(
            listOf(
                Participant(
                    actor =
                    Reference(
                        reference = "Patient/${tenant.mnemonic}-patientFhirId",
                        display = "Patient Name",
                        type = Uri("Patient")
                    ),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor =
                    Reference(
                        reference = "Practitioner/${tenant.mnemonic}-practitionerFhirId",
                        display = "Coordinator Phoenix, RN",
                        type = Uri("Practitioner")
                    ),
                    status = ParticipationStatus.ACCEPTED
                )
            ),

            oncologyAppointment.participant
        )
    }

    @Test
    fun `transform appointment works when we already have systems`() {
        val mockPatientIdentifier = mockk<EHRIdentifier> {
            every { value } returns "patientEpicId"
            every { type?.text } returns "External"
            every { system } returns "mrnSystem"
        }
        val mockPatientReference = mockk<EpicPatientReference> {
            every { display } returns "Patient Name"
            every { identifier } returns mockPatientIdentifier
            every { type } returns "Patient"
        }
        val patientParticipant = mockk<EpicPatientParticipant> {
            every { actor } returns mockPatientReference
        }
        val mockProviderIdentifier = mockk<EHRIdentifier> {
            every { value } returns "providerId"
            every { type?.text } returns "External"
            every { system } returns null
        }
        val mockProviderReference = mockk<EpicProviderReference> {
            every { display } returns "Coordinator Phoenix, RN"
            every { identifier } returns mockProviderIdentifier
            every { type } returns "Practitioner"
        }
        val providerParticipant = mockk<EpicProviderParticipant> {
            every { actor } returns mockProviderReference
        }
        val mockParticipants = listOf<EHRParticipant>(patientParticipant, providerParticipant)
        val appointment = mockk<EpicAppointment>() {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns mockParticipants
        }

        every {
            mockPatientService.getPatientFHIRIds(
                "test",
                mapOf("any" to SystemValue("patientEpicId", "mrnSystem"))
            )
        } returns mapOf("any" to "test-patientFhirId")

        every {
            mockPractitionerService.getPractitionerFHIRIds(
                "test",
                mapOf(mockProviderIdentifier to SystemValue("providerId", ""))
            )
        } returns mapOf(mockProviderIdentifier to "practitionerFhirId")
        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)
        assertNotNull(oncologyAppointment)
        oncologyAppointment!!

        assertEquals(
            listOf(
                Participant(
                    actor =
                    Reference(
                        reference = "Patient/${tenant.mnemonic}-patientFhirId",
                        display = "Patient Name",
                        type = Uri("Patient")
                    ),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor =
                    Reference(
                        reference = "Practitioner/${tenant.mnemonic}-practitionerFhirId",
                        display = "Coordinator Phoenix, RN",
                        type = Uri("Practitioner")
                    ),
                    status = ParticipationStatus.ACCEPTED
                )
            ),

            oncologyAppointment.participant
        )
    }

    @Test
    fun `transform handles when patient isn't found in aidbox`() {
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

        val appointment = mockk<EpicAppointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns mockParticipants
        }

        every {
            mockPatientService.getPatientFHIRIds(
                "test",
                mapOf("any" to SystemValue("patientEpicId", ""))
            )
        } returns emptyMap()

        every {
            mockPractitionerService.getPractitionerFHIRIds(
                "test",
                mapOf(mockProviderIdentifier to SystemValue("providerId", ""))
            )
        } returns mapOf(mockProviderIdentifier to "practitionerFhirId")
        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)
        val patientParticipantResult = oncologyAppointment?.participant?.first {
            it.actor?.type?.value == ReferenceTypes.PATIENT
        }

        assertNull(patientParticipantResult?.actor?.id)
        assertNotNull(patientParticipantResult?.actor?.identifier)
    }

    @Test
    fun `transform handles when patient doesn't have an identifier`() {
        val mockPatientReference = mockk<EpicPatientReference> {
            every { display } returns "Patient Name"
            every { identifier } returns null
            every { type } returns "Patient"
        }
        val patientParticipant = mockk<EpicPatientParticipant> {
            every { actor } returns mockPatientReference
        }
        val appointment = mockk<EpicAppointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
            every { resource } returns aoAppointment
            every { participants } returns listOf(patientParticipant)
        }

        every {
            mockPatientService.getPatientFHIRIds(
                "test",
                mapOf("any" to SystemValue("patientEpicId", ""))
            )
        } returns emptyMap()

        every {
            mockPractitionerService.getPractitionerFHIRIds(
                "test",
                mapOf(mockProviderIdentifier to SystemValue("providerId", ""))
            )
        } returns mapOf(mockProviderIdentifier to "practitionerFhirId")
        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)
        val patientParticipantResult = oncologyAppointment?.participant?.first {
            it.actor?.type?.value == ReferenceTypes.PATIENT
        }

        assertNull(patientParticipantResult?.actor?.id)
    }
}
