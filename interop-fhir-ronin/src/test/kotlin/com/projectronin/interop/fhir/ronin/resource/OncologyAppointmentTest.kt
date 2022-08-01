package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.ExtensionMeanings
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
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OncologyAppointmentTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `validate fails if no tenant identifier provided`() {
        val appointment = Appointment(
            extension = listOf(
                Extension(
                    url = ExtensionMeanings.PARTNER_DEPARTMENT.uri,
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            identifier = listOf(),
            status = AppointmentStatus.PROPOSED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyAppointment.validate(appointment).alertIfErrors()
        }
        assertEquals("Tenant identifier is required", exception.message)
    }

    @Test
    fun `validate fails if partnerReference value is not a Reference`() {
        val appointment = Appointment(
            extension = listOf(
                Extension(
                    url = ExtensionMeanings.PARTNER_DEPARTMENT.uri,
                    value = DynamicValue(DynamicValueType.STRING, "reference")
                )
            ),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            status = AppointmentStatus.PROPOSED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyAppointment.validate(appointment).alertIfErrors()
        }
        assertEquals("Partner department reference must be of type Reference", exception.message)
    }

    @Test
    fun `validate fails if partnerReference value is missing`() {
        val appointment = Appointment(
            extension = listOf(
                Extension(
                    url = ExtensionMeanings.PARTNER_DEPARTMENT.uri
                )
            ),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            status = AppointmentStatus.PROPOSED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyAppointment.validate(appointment).alertIfErrors()
        }
        assertEquals("Partner department reference must be of type Reference", exception.message)
    }

    @Test
    fun `validate fails for multiple issues`() {
        val appointment = Appointment(
            extension = listOf(
                Extension(
                    url = ExtensionMeanings.PARTNER_DEPARTMENT.uri,
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(),
            status = AppointmentStatus.PROPOSED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyAppointment.validate(appointment).alertIfErrors()
        }
        assertEquals(
            "Encountered multiple validation errors:\nTenant identifier is required\nPartner department reference must be of type Reference",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if partnerReference is missing`() {
        val appointment = Appointment(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
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

        OncologyAppointment.validate(appointment)
    }

    @Test
    fun `transforms appointment with all attributes`() {
        val appointment = Appointment(
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

        val transformed = OncologyAppointment.transform(appointment, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Appointment", transformed.resourceType)
        assertEquals(Id(value = "test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner"))),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), transformed.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
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
                Identifier(value = "id"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertEquals(AppointmentStatus.CANCELLED, transformed.status)
        assertEquals(CodeableConcept(text = "cancel reason"), transformed.cancelationReason)
        assertEquals((listOf(CodeableConcept(text = "service category"))), transformed.serviceCategory)
        assertEquals((listOf(CodeableConcept(text = "service type"))), transformed.serviceType)
        assertEquals((listOf(CodeableConcept(text = "specialty"))), transformed.specialty)
        assertEquals(CodeableConcept(text = "appointment type"), transformed.appointmentType)
        assertEquals(listOf(CodeableConcept(text = "reason code")), transformed.reasonCode)
        assertEquals(listOf(Reference(display = "reason reference")), transformed.reasonReference)
        assertEquals(1, transformed.priority)
        assertEquals("appointment test", transformed.description)
        assertEquals(listOf(Reference(display = "supporting info")), transformed.supportingInformation)
        assertEquals(Instant(value = "2017-01-01T00:00:00Z"), transformed.start)
        assertEquals(Instant(value = "2017-01-01T01:00:00Z"), transformed.end)
        assertEquals(15, transformed.minutesDuration)
        assertEquals(listOf(Reference(display = "slot")), transformed.slot)
        assertEquals(DateTime(value = "2021-11-16"), transformed.created)
        assertEquals("patient instruction", transformed.patientInstruction)
        assertEquals(listOf(Reference(display = "based on")), transformed.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
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
            status = AppointmentStatus.CANCELLED,
            participant = listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        )

        val transformed = OncologyAppointment.transform(appointment, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Appointment", transformed.resourceType)
        assertEquals(Id(value = "test-12345"), transformed.id)
        Assertions.assertNull(transformed.meta)
        Assertions.assertNull(transformed.implicitRules)
        Assertions.assertNull(transformed.language)
        Assertions.assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertEquals(AppointmentStatus.CANCELLED, transformed.status)
        Assertions.assertNull(transformed.cancelationReason)
        assertEquals(listOf<CodeableConcept>(), transformed.serviceCategory)
        assertEquals(listOf<CodeableConcept>(), transformed.serviceType)
        assertEquals(listOf<CodeableConcept>(), transformed.specialty)
        Assertions.assertNull(transformed.appointmentType)
        assertEquals(listOf<CodeableConcept>(), transformed.reasonCode)
        assertEquals(listOf<Reference>(), transformed.reasonReference)
        Assertions.assertNull(transformed.priority)
        Assertions.assertNull(transformed.description)
        assertEquals(listOf<Reference>(), transformed.supportingInformation)
        Assertions.assertNull(transformed.start)
        Assertions.assertNull(transformed.end)
        Assertions.assertNull(transformed.minutesDuration)
        assertEquals(listOf<Reference>(), transformed.slot)
        Assertions.assertNull(transformed.created)
        Assertions.assertNull(transformed.patientInstruction)
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(
            listOf(
                Participant(
                    actor = Reference(display = "actor"),
                    status = ParticipationStatus.ACCEPTED
                )
            ),
            transformed.participant
        )
        assertEquals(listOf<Period>(), transformed.requestedPeriod)
    }

    @Test
    fun `transform fails for appointment with missing id`() {
        val appointment = Appointment(
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

        val transformed = OncologyAppointment.transform(appointment, tenant)

        Assertions.assertNull(transformed)
    }
}
