package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.validate.resource.R4AppointmentValidator
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninAppointmentTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `always qualifies`() {
        assertTrue(
            RoninAppointment.qualifies(
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
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninAppointment.validate(appointment, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Appointment.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Appointment.identifier",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val appointment = Appointment(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor".asFHIR()),
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
            RoninAppointment.validate(appointment, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: basedOn is a required element @ Appointment.basedOn",
            exception.message
        )

        unmockkObject(R4AppointmentValidator)
    }

    @Test
    fun `validate succeeds`() {
        val appointment = Appointment(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        RoninAppointment.validate(appointment, null).alertIfErrors()
    }

    @Test
    fun `transforms appointment with all attributes`() {
        val appointment = Appointment(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/appointment.html"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
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
            priority = 1.asFHIR(),
            description = "appointment test".asFHIR(),
            supportingInformation = listOf(Reference(display = "supporting info".asFHIR())),
            start = Instant("2017-01-01T00:00:00Z"),
            end = Instant("2017-01-01T01:00:00Z"),
            minutesDuration = 15.asFHIR(),
            slot = listOf(Reference(display = "slot".asFHIR())),
            created = DateTime("2021-11-16"),
            comment = "comment".asFHIR(),
            patientInstruction = "patient instruction".asFHIR(),
            basedOn = listOf(Reference(display = "based on".asFHIR())),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            ),
            requestedPeriod = listOf(Period(start = DateTime("2021-11-16")))
        )

        val transformed = RoninAppointment.transform(appointment, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Appointment", transformed.resourceType)
        assertEquals(Id(value = "test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value))),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
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
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
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
        assertEquals(1.asFHIR(), transformed.priority)
        assertEquals("appointment test".asFHIR(), transformed.description)
        assertEquals(listOf(Reference(display = "supporting info".asFHIR())), transformed.supportingInformation)
        assertEquals(Instant(value = "2017-01-01T00:00:00Z"), transformed.start)
        assertEquals(Instant(value = "2017-01-01T01:00:00Z"), transformed.end)
        assertEquals(15.asFHIR(), transformed.minutesDuration)
        assertEquals(listOf(Reference(display = "slot".asFHIR())), transformed.slot)
        assertEquals(DateTime(value = "2021-11-16"), transformed.created)
        assertEquals("patient instruction".asFHIR(), transformed.patientInstruction)
        assertEquals(listOf(Reference(display = "based on".asFHIR())), transformed.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = Reference(display = "actor".asFHIR()),
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
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val transformed = RoninAppointment.transform(appointment, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Appointment", transformed.resourceType)
        assertEquals(Id(value = "test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.APPOINTMENT.value))),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
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
                    actor = Reference(display = "actor".asFHIR()),
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
            status = AppointmentStatus.CANCELLED.asCode(),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                )
            )
        )

        val transformed = RoninAppointment.transform(appointment, tenant)

        assertNull(transformed)
    }
}
