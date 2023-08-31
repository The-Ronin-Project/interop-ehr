package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.validate.resource.R4AppointmentValidator
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCoding
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninAppointmentTest {
    private lateinit var registryClient: NormalizationRegistryClient
    private lateinit var normalizer: Normalizer
    private lateinit var localizer: Localizer
    private lateinit var roninAppointment: RoninAppointment

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val conceptMapMetadata = ConceptMapMetadata(
        registryEntryType = "concept-map",
        conceptMapName = "test-concept-map",
        conceptMapUuid = "573b456efca5-03d51d53-1a31-49a9-af74",
        version = "1"
    )
    private val valueSetMetadata = ValueSetMetadata(
        registryEntryType = "value_set",
        valueSetName = "test-value-set",
        valueSetUuid = "03d51d53-1a31-49a9-af74-573b456efca5",
        version = "2"
    )

    @BeforeEach
    fun setup() {
        registryClient = mockk()
        normalizer = mockk {
            every { normalize(any(), tenant) } answers { firstArg() }
        }
        localizer = mockk {
            every { localize(any(), tenant) } answers { firstArg() }
        }
        roninAppointment = RoninAppointment(registryClient, normalizer, localizer)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninAppointment.qualifies(
                Appointment(
                    status = AppointmentStatus.CANCELLED.asCode(),
                    participant = listOf(
                        Participant(
                            actor = Reference(display = "actor".asFHIR()),
                            status = ParticipationStatus.ACCEPTED.asCode()
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            extension = listOf(statusExtension("cancelled")),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Appointment.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Appointment.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ Appointment.identifier",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(statusExtension("cancelled")),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        mockkObject(R4AppointmentValidator)
        every { R4AppointmentValidator.validate(appointment, LocationContext(Appointment::class)) } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Appointment::basedOn),
                LocationContext(Appointment::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: basedOn is a required element @ Appointment.basedOn",
            exception.message
        )

        unmockkObject(R4AppointmentValidator)
    }

    @Test
    fun `validate checks meta`() {
        val appointment = Appointment(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(statusExtension("cancelled")),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ Appointment.meta",
            exception.message
        )
    }

    @Test
    fun `validate checks actor reference`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(statusExtension("cancelled")),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(display = "Practitioner/actor".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not one of Patient, PractitionerRole, Practitioner, Location, RelatedPerson, Device, HealthcareService @ Participant.actor\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ Participant.actor.type",
            exception.message
        )
    }

    @Test
    fun `validate succeeds`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(statusExtension("cancelled")),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        roninAppointment.validate(appointment).alertIfErrors()
    }

    @Test
    fun `transforms appointment with all attributes`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/appointment.html")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(Location(id = Id("67890"))),
            extension = listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                ),
                Extension(
                    url = Uri("http://hl7.org/extension-2"),
                    value = DynamicValue(DynamicValueType.BOOLEAN, false)
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id".asFHIR())),
            status = AppointmentStatus.CANCELLED.asCode(),
            cancelationReason = CodeableConcept(text = "cancel reason".asFHIR()),
            serviceCategory = listOf(CodeableConcept(text = "service category".asFHIR())),
            serviceType = listOf(CodeableConcept(text = "service type".asFHIR())),
            specialty = listOf(CodeableConcept(text = "specialty".asFHIR())),
            appointmentType = CodeableConcept(text = "appointment type".asFHIR()),
            reasonCode = listOf(CodeableConcept(text = "reason code".asFHIR())),
            reasonReference = listOf(Reference(display = "reason reference".asFHIR())),
            priority = UnsignedInt(1),
            description = "appointment test".asFHIR(),
            supportingInformation = listOf(Reference(display = "supporting info".asFHIR())),
            start = Instant("2017-01-01T00:00:00Z"),
            end = Instant("2017-01-01T01:00:00Z"),
            minutesDuration = PositiveInt(15),
            slot = listOf(Reference(display = "slot".asFHIR())),
            created = DateTime("2021-11-16"),
            comment = "comment".asFHIR(),
            patientInstruction = "patient instruction".asFHIR(),
            basedOn = listOf(Reference(display = "based on".asFHIR())),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            ),
            requestedPeriod = listOf(Period(start = DateTime("2021-11-16")))
        )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                    code = Code(value = "cancelled")
                ),
                AppointmentStatus::class,
                RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value
            )
        } returns ConceptMapCoding(statusCoding("cancelled"), statusExtension("cancelled"), listOf(conceptMapMetadata))

        val (transformed, validation) = roninAppointment.transform(appointment, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Appointment", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                ),
                Extension(
                    url = Uri("http://hl7.org/extension-2"),
                    value = DynamicValue(DynamicValueType.BOOLEAN, false)
                ),
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                            code = Code(value = "cancelled")
                        )
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(AppointmentStatus.CANCELLED.asCode(), transformed.status)
        assertEquals(CodeableConcept(text = "cancel reason".asFHIR()), transformed.cancelationReason)
        assertEquals((listOf(CodeableConcept(text = "service category".asFHIR()))), transformed.serviceCategory)
        assertEquals((listOf(CodeableConcept(text = "service type".asFHIR()))), transformed.serviceType)
        assertEquals((listOf(CodeableConcept(text = "specialty".asFHIR()))), transformed.specialty)
        assertEquals(CodeableConcept(text = "appointment type".asFHIR()), transformed.appointmentType)
        assertEquals(listOf(CodeableConcept(text = "reason code".asFHIR())), transformed.reasonCode)
        assertEquals(listOf(Reference(display = "reason reference".asFHIR())), transformed.reasonReference)
        assertEquals(UnsignedInt(1), transformed.priority)
        assertEquals("appointment test".asFHIR(), transformed.description)
        assertEquals(listOf(Reference(display = "supporting info".asFHIR())), transformed.supportingInformation)
        assertEquals(Instant(value = "2017-01-01T00:00:00Z"), transformed.start)
        assertEquals(Instant(value = "2017-01-01T01:00:00Z"), transformed.end)
        assertEquals(PositiveInt(15), transformed.minutesDuration)
        assertEquals(listOf(Reference(display = "slot".asFHIR())), transformed.slot)
        assertEquals(DateTime(value = "2021-11-16"), transformed.created)
        assertEquals("patient instruction".asFHIR(), transformed.patientInstruction)
        assertEquals(listOf(Reference(display = "based on".asFHIR())), transformed.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            ),
            transformed.participant
        )
        assertEquals(listOf(Period(start = DateTime(value = "2021-11-16"))), transformed.requestedPeriod)
    }

    @Test
    fun `transform appointment with only required attributes`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                    code = Code(value = "cancelled")
                ),
                AppointmentStatus::class,
                RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value
            )
        } returns ConceptMapCoding(statusCoding("cancelled"), statusExtension("cancelled"), listOf(conceptMapMetadata))

        val (transformed, validation) = roninAppointment.transform(appointment, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Appointment", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                            code = Code(value = "cancelled")
                        )
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(AppointmentStatus.CANCELLED.asCode(), transformed.status)
        assertNull(transformed.cancelationReason)
        assertEquals(listOf<CodeableConcept>(), transformed.serviceCategory)
        assertEquals(listOf<CodeableConcept>(), transformed.serviceType)
        assertEquals(listOf<CodeableConcept>(), transformed.specialty)
        assertNull(transformed.appointmentType)
        assertEquals(listOf<CodeableConcept>(), transformed.reasonCode)
        assertEquals(listOf<Reference>(), transformed.reasonReference)
        assertNull(transformed.priority)
        assertNull(transformed.description)
        assertEquals(listOf<Reference>(), transformed.supportingInformation)
        assertNull(transformed.start)
        assertNull(transformed.end)
        assertNull(transformed.minutesDuration)
        assertEquals(listOf<Reference>(), transformed.slot)
        assertNull(transformed.created)
        assertNull(transformed.patientInstruction)
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            ),
            transformed.participant
        )
        assertEquals(listOf<Period>(), transformed.requestedPeriod)
    }

    @Test
    fun `transform fails for appointment with missing id`() {
        val appointment = Appointment(
            identifier = listOf(Identifier(value = "id".asFHIR())),
            extension = listOf(statusExtension("cancelled")),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                    code = Code(value = "cancelled")
                ),
                AppointmentStatus::class,
                RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value
            )
        } returns ConceptMapCoding(statusCoding("cancelled"), statusExtension("cancelled"), listOf(conceptMapMetadata))

        val (transformed, _) = roninAppointment.transform(appointment, tenant)
        assertNull(transformed)
    }

    @Test
    fun `validate fails for appointment with missing identifiers`() {
        val appointment = Appointment(
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            identifier = listOf(Identifier(value = "id".asFHIR())),
            extension = listOf(statusExtension("cancelled")),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment, LocationContext(Appointment::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Appointment.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Appointment.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ Appointment.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails for appointment with wrong status for missing start and end for participant`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(statusExtension("booked")),
            status = AppointmentStatus.BOOKED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment, LocationContext(Appointment::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR R4_APPT_002: Start and end can only be missing for appointments with the following statuses: proposed, cancelled, waitlist @ Appointment",
            exception.message
        )
    }

    @Test
    fun `validate fails for appointment with wrong status for cancelationReason`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(statusExtension("proposed")),
            status = AppointmentStatus.PROPOSED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            ),
            cancelationReason = CodeableConcept(
                text = "No Show".asFHIR(),
                coding = listOf(Coding(code = AppointmentStatus.NOSHOW.asCode()))
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment, LocationContext(Appointment::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR R4_APPT_003: cancelationReason is only used for appointments that have the following statuses: cancelled, noshow @ Appointment",
            exception.message
        )
    }

    @Test
    fun `validate fails for appointment missing status source extension`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment, LocationContext(Appointment::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_APPT_001: Appointment extension list may not be empty @ Appointment.status",
            exception.message
        )
    }

    @Test
    fun `validate fails for appointment with wrong URL in status source extension`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(
                Extension(
                    url = Uri("emailSystemExtension"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = statusCoding("cancelled")
                    )
                )
            ),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment, LocationContext(Appointment::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_APPT_002: Tenant source appointment status extension is missing or invalid @ Appointment.status",
            exception.message
        )
    }

    @Test
    fun `validate fails for appointment with missing URL in status source extension`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(
                Extension(
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = statusCoding("cancelled")
                    )
                )
            ),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment, LocationContext(Appointment::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_APPT_002: Tenant source appointment status extension is missing or invalid @ Appointment.status\n" +
                "ERROR REQ_FIELD: url is a required element @ Appointment.extension[0].url",
            exception.message
        )
    }

    @Test
    fun `validate fails for appointment with right URL and wrong data type in status source extension`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                    value = DynamicValue(
                        type = DynamicValueType.BOOLEAN,
                        value = true
                    )
                )
            ),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninAppointment.validate(appointment, LocationContext(Appointment::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_APPT_002: Tenant source appointment status extension is missing or invalid @ Appointment.status",
            exception.message
        )
    }

    @Test
    fun `transform succeeds for appointment status - when concept map returns a good value`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),

                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = Code(value = "abc"),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                    code = Code(value = "abc")
                ),
                AppointmentStatus::class,
                RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value
            )
        } returns ConceptMapCoding(statusCoding("cancelled"), statusExtension("abc"), listOf(conceptMapMetadata))

        val (transformed, validation) = roninAppointment.transform(appointment, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                            code = Code(value = "abc")
                        )
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(
            Code(value = "cancelled"),
            transformed.status
        )
    }

    @Test
    fun `transform fails for appointment status - when concept map has no match - and source code is not in enum`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = Code(value = "xyz"),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                    code = Code(value = "xyz")
                ),
                AppointmentStatus::class,
                RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value
            )
        } returns null

        val pair = roninAppointment.transform(appointment, tenant)
        val exception = assertThrows<IllegalArgumentException> {
            pair.second.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'xyz' has no target " +
                "defined in http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus " +
                "@ Appointment.status\n" +
                "ERROR RONIN_APPT_001: Appointment extension list may not be empty @ Appointment.status\n" +
                "ERROR INV_VALUE_SET: 'xyz' is outside of required value set @ Appointment.status",
            exception.message
        )
    }

    @Test
    fun `transform fails for appointment status - when concept map has no match - and source code is in enum`() {
        // see NormalizationRegistryClientTest @Test
        // fun `getConceptMappingForEnum with no matching registry - and source value is good for enum - returns enum as Coding`()

        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = Code(value = "cancelled"),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Practitioner/actor".asFHIR(), type = Uri("Practitioner", extension = dataAuthorityExtension)),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )
        val sourceCoding = Coding(
            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
            code = Code(value = "cancelled")
        )
        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                sourceCoding,
                AppointmentStatus::class,
                RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value
            )
        } returns ConceptMapCoding(
            Coding(
                system = Uri("http://projectronin.io/fhir/CodeSystem/AppointmentStatus"),
                code = Code(value = "cancelled")
            ),
            Extension(
                url = Uri(RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value),
                value = DynamicValue(
                    DynamicValueType.CODING,
                    value = sourceCoding
                )
            ),
            listOf(conceptMapMetadata)
        )

        val (transformed, validation) = roninAppointment.transform(appointment, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = sourceCoding
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(
            Code(value = "cancelled"),
            transformed.status
        )
    }

    @Test
    fun `transform fails if the concept map result for status invalidates an invariant for Appointment - start and end`() {
        val appointment = Appointment(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(statusExtension("cancelled")),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                    code = Code(value = "cancelled")
                ),
                AppointmentStatus::class,
                RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value
            )
        } returns ConceptMapCoding(statusCoding("booked"), statusExtension("cancelled"), listOf(conceptMapMetadata))

        val (transformed, _) = roninAppointment.transform(appointment, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform fails if the concept map result for status invalidates an invariant for Appointment - cancelation reason`() {
        val appointment = Appointment(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = AppointmentStatus.PROPOSED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            ),
            cancelationReason = CodeableConcept(
                text = "No Show".asFHIR(),
                coding = listOf(Coding(code = AppointmentStatus.NOSHOW.asCode()))
            )
        )

        every {
            registryClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                Coding(
                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                    code = Code(value = "proposed")
                ),
                AppointmentStatus::class,
                RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value
            )
        } returns ConceptMapCoding(statusCoding("waitlist"), statusExtension("proposed"), listOf(conceptMapMetadata))

        val (transformed, _) = roninAppointment.transform(appointment, tenant)
        assertNull(transformed)
    }

    private fun statusCoding(value: String) = Coding(
        system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
        code = Code(value = value)
    )

    private fun statusExtension(value: String) = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
        value = DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                code = Code(value = value)
            )
        )
    )
}
