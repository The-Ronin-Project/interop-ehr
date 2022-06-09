package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
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
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.fhir.r4.resource.Appointment as R4Appointment

class R4AppointmentTransformerTest {
    private val transformer = R4AppointmentTransformer()

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `transforms appointment with all attributes`() {
        val r4Appointment = R4Appointment(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id")),
            status = AppointmentStatus.CANCELLED,
            cancelationReason = CodeableConcept(text = "cancel reason"),
            serviceCategory = listOf(CodeableConcept(text = "service category")),
            serviceType = listOf(CodeableConcept(text = "service type")),
            specialty = listOf(CodeableConcept(text = "specialty")),
            appointmentType = CodeableConcept(text = "appointment type"),
            reasonCode = listOf(CodeableConcept(text = "reason code")),
            reasonReference = listOf(Reference(display = "reason reference")),
            priority = 1,
            description = "appointment test",
            supportingInformation = listOf(Reference(display = "supporting info")),
            start = Instant("2017-01-01T00:00:00Z"),
            end = Instant("2017-01-01T01:00:00Z"),
            minutesDuration = 15,
            slot = listOf(Reference(display = "slot")),
            created = DateTime("2021-11-16"),
            comment = "comment",
            patientInstruction = "patient instruction",
            basedOn = listOf(Reference(display = "based on")),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            ),
            requestedPeriod = listOf(Period(start = DateTime("2021-11-16")))
        )

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Appointment
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)

        oncologyAppointment!! // Force it to be treated as non-null
        assertEquals("Appointment", oncologyAppointment.resourceType)
        assertEquals(Id(value = "test-12345"), oncologyAppointment.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner"))),
            oncologyAppointment.meta
        )
        assertEquals(Uri("implicit-rules"), oncologyAppointment.implicitRules)
        assertEquals(Code("en-US"), oncologyAppointment.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), oncologyAppointment.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            oncologyAppointment.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            oncologyAppointment.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyAppointment.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyAppointment.identifier
        )
        assertEquals(AppointmentStatus.CANCELLED, oncologyAppointment.status)
        assertEquals(CodeableConcept(text = "cancel reason"), oncologyAppointment.cancelationReason)
        assertEquals((listOf(CodeableConcept(text = "service category"))), oncologyAppointment.serviceCategory)
        assertEquals((listOf(CodeableConcept(text = "service type"))), oncologyAppointment.serviceType)
        assertEquals((listOf(CodeableConcept(text = "specialty"))), oncologyAppointment.specialty)
        assertEquals(CodeableConcept(text = "appointment type"), oncologyAppointment.appointmentType)
        assertEquals(listOf(CodeableConcept(text = "reason code")), oncologyAppointment.reasonCode)
        assertEquals(listOf(Reference(display = "reason reference")), oncologyAppointment.reasonReference)
        assertEquals(1, oncologyAppointment.priority)
        assertEquals("appointment test", oncologyAppointment.description)
        assertEquals(listOf(Reference(display = "supporting info")), oncologyAppointment.supportingInformation)
        assertEquals(Instant(value = "2017-01-01T00:00:00Z"), oncologyAppointment.start)
        assertEquals(Instant(value = "2017-01-01T01:00:00Z"), oncologyAppointment.end)
        assertEquals(15, oncologyAppointment.minutesDuration)
        assertEquals(listOf(Reference(display = "slot")), oncologyAppointment.slot)
        assertEquals(DateTime(value = "2021-11-16"), oncologyAppointment.created)
        assertEquals("patient instruction", oncologyAppointment.patientInstruction)
        assertEquals(listOf(Reference(display = "based on")), oncologyAppointment.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            ),
            oncologyAppointment.participant
        )
        assertEquals(listOf(Period(start = DateTime(value = "2021-11-16"))), oncologyAppointment.requestedPeriod)
    }

    @Test
    fun `transform appointment with only required attributes`() {
        val r4Appointment = R4Appointment(
            id = Id("12345"),
            status = AppointmentStatus.CANCELLED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Appointment
        }

        val oncologyAppointment = transformer.transformAppointment(appointment, tenant)

        oncologyAppointment!! // Force it to be treated as non-null
        assertEquals("Appointment", oncologyAppointment.resourceType)
        assertEquals(Id(value = "test-12345"), oncologyAppointment.id)
        assertNull(oncologyAppointment.meta)
        assertNull(oncologyAppointment.implicitRules)
        assertNull(oncologyAppointment.language)
        assertNull(oncologyAppointment.text)
        assertEquals(listOf<ContainedResource>(), oncologyAppointment.contained)
        assertEquals(listOf<Extension>(), oncologyAppointment.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyAppointment.identifier
        )
        assertEquals(AppointmentStatus.CANCELLED, oncologyAppointment.status)
        assertNull(oncologyAppointment.cancelationReason)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.serviceCategory)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.serviceType)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.specialty)
        assertNull(oncologyAppointment.appointmentType)
        assertEquals(listOf<CodeableConcept>(), oncologyAppointment.reasonCode)
        assertEquals(listOf<Reference>(), oncologyAppointment.reasonReference)
        assertNull(oncologyAppointment.priority)
        assertNull(oncologyAppointment.description)
        assertEquals(listOf<Reference>(), oncologyAppointment.supportingInformation)
        assertNull(oncologyAppointment.start)
        assertNull(oncologyAppointment.end)
        assertNull(oncologyAppointment.minutesDuration)
        assertEquals(listOf<Reference>(), oncologyAppointment.slot)
        assertNull(oncologyAppointment.created)
        assertNull(oncologyAppointment.patientInstruction)
        assertEquals(listOf<Reference>(), oncologyAppointment.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            ),
            oncologyAppointment.participant
        )
        assertEquals(listOf<Period>(), oncologyAppointment.requestedPeriod)
    }

    @Test
    fun `non R4 appointment`() {
        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformAppointment(appointment, tenant)
        }

        assertEquals("Appointment is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `fails for appointment with missing id`() {
        val r4Appointment = R4Appointment(
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            status = AppointmentStatus.CANCELLED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Appointment
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `fails partnerReference extension not being a Reference`() {
        val r4Appointment = R4Appointment(
            id = Id("12345"),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.BOOLEAN, false)
                )
            ),
            status = AppointmentStatus.CANCELLED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )

        val appointment = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Appointment
        }

        assertNull(transformer.transformAppointment(appointment, tenant))
    }

    @Test
    fun `non R4 bundle`() {
        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformAppointments(bundle, tenant)
        }

        assertEquals("Bundle is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `bundle transformation returns empty when no valid transformations`() {
        val invalidAppointment = R4Appointment(
            id = Id("12345"),
            status = AppointmentStatus.CANCELLED,
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.BOOLEAN, false)
                )
            ),
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )
        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidAppointment
        }

        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidAppointment
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(0, oncologyAppointments.size)
    }

    @Test
    fun `bundle transformation returns only valid transformations`() {
        val invalidAppointment = R4Appointment(
            id = Id("12345"),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.BOOLEAN, false)
                )
            ),
            status = AppointmentStatus.CANCELLED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )
        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidAppointment
        }

        val r4Appointment2 = R4Appointment(
            id = Id("12345"),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            status = AppointmentStatus.CANCELLED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )
        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Appointment2
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(1, oncologyAppointments.size)
    }

    @Test
    fun `bundle transformation returns all when all valid`() {
        val r4Appointment1 = R4Appointment(
            id = Id("12345"),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            status = AppointmentStatus.CANCELLED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )

        val appointment1 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Appointment1
        }

        val appointment2 = mockk<Appointment> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Appointment1
        }

        val bundle = mockk<Bundle<Appointment>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(appointment1, appointment2)
        }

        val oncologyAppointments = transformer.transformAppointments(bundle, tenant)
        assertEquals(2, oncologyAppointments.size)
    }
}
