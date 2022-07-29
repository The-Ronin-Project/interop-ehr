package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.AvailableTime
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.NotAvailable
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OncologyPractitionerRoleTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `fails if no tenant identifier provided`() {
        val practitionerRole = PractitionerRole(
            identifier = listOf(),
            practitioner = Reference(reference = "Practitioner/1234"),
            organization = Reference(reference = "Organization/1234")
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyPractitionerRole.validate(practitionerRole)
        }
        assertEquals("Tenant identifier is required", exception.message)
    }

    @Test
    fun `fails if tenant does not have tenant codeable concept`() {
        val practitionerRole = PractitionerRole(
            identifier = listOf(Identifier(system = CodeSystem.RONIN_TENANT.uri, type = CodeableConcepts.SER)),
            practitioner = Reference(reference = "Practitioner/1234"),
            organization = Reference(reference = "Organization/1234")
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyPractitionerRole.validate(practitionerRole)
        }
        assertEquals("Tenant identifier provided without proper CodeableConcept defined", exception.message)
    }

    @Test
    fun `fails if telecom missing system`() {
        val practitionerRole = PractitionerRole(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            practitioner = Reference(reference = "Practitioner/1234"),
            organization = Reference(reference = "Organization/1234"),
            // This is also missing a value, but a ContactPoint cannot currently be created without a system if a value is present.
            telecom = listOf(ContactPoint(use = ContactPointUse.HOME))
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyPractitionerRole.validate(practitionerRole)
        }
        assertEquals("All PractitionerRole telecoms require a value and system", exception.message)
    }

    @Test
    fun `fails if telecom missing value`() {
        val practitionerRole = PractitionerRole(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            practitioner = Reference(reference = "Practitioner/1234"),
            organization = Reference(reference = "Organization/1234"),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE))
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyPractitionerRole.validate(practitionerRole)
        }
        assertEquals("All PractitionerRole telecoms require a value and system", exception.message)
    }

    @Test
    fun `transform fails for practitioner role with no ID`() {
        val practitionerRole = PractitionerRole()

        val transformed = OncologyPractitionerRole.transform(practitionerRole, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform fails for practitioner role with no practitioner`() {
        val practitionerRole = PractitionerRole(
            id = Id("1234"),
            organization = Reference(reference = "Organization/1234")
        )

        val transformed = OncologyPractitionerRole.transform(practitionerRole, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transforms a sparse practitioner role with all required properties but no organization, location, or other optional properties`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitionerrole"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            identifier = listOf(Identifier(value = "id")),
            active = true,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/1234"),
            code = listOf(CodeableConcept(text = "code")),
            specialty = listOf(CodeableConcept(text = "specialty")),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
            availableTime = listOf(AvailableTime(allDay = false)),
            notAvailable = listOf(NotAvailable(description = "Not available now")),
            availabilityExceptions = "exceptions",
            endpoint = listOf(Reference(reference = "Endpoint/1357"))
        )

        val transformed = OncologyPractitionerRole.transform(practitionerRole, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitionerrole"))),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), transformed.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            transformed.contained
        )
        assertEquals(0, transformed.extension.size)
        assertEquals(0, transformed.modifierExtension.size)
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test"
                )
            ),
            transformed.identifier
        )
        assertEquals(true, transformed.active)
        assertEquals(Period(end = DateTime("2022")), transformed.period)
        assertEquals(Reference(reference = "Practitioner/test-1234"), transformed.practitioner)
        assertNull(transformed.organization)
        assertEquals(listOf(CodeableConcept(text = "code")), transformed.code)
        assertEquals(listOf(CodeableConcept(text = "specialty")), transformed.specialty)
        assertEquals(0, transformed.location.size)
        assertEquals(0, transformed.healthcareService.size)
        assertEquals(
            listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
            transformed.telecom
        )
        assertEquals(listOf(AvailableTime(allDay = false)), transformed.availableTime)
        assertEquals(listOf(NotAvailable(description = "Not available now")), transformed.notAvailable)
        assertEquals("exceptions", transformed.availabilityExceptions)
        assertEquals(listOf(Reference(reference = "Endpoint/test-1357")), transformed.endpoint)
    }

    @Test
    fun `transforms practitioner role with all attributes`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitionerrole"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id")),
            active = true,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/1234"),
            organization = Reference(reference = "Organization/5678"),
            code = listOf(CodeableConcept(text = "code")),
            specialty = listOf(CodeableConcept(text = "specialty")),
            location = listOf(Reference(reference = "Location/9012")),
            healthcareService = listOf(Reference(reference = "HealthcareService/3456")),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
            availableTime = listOf(AvailableTime(allDay = false)),
            notAvailable = listOf(NotAvailable(description = "Not available now")),
            availabilityExceptions = "exceptions",
            endpoint = listOf(Reference(reference = "Endpoint/1357"))
        )

        val transformed = OncologyPractitionerRole.transform(practitionerRole, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitionerrole"))),
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
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
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
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test"
                )
            ),
            transformed.identifier
        )
        assertEquals(true, transformed.active)
        assertEquals(Period(end = DateTime("2022")), transformed.period)
        assertEquals(Reference(reference = "Practitioner/test-1234"), transformed.practitioner)
        assertEquals(Reference(reference = "Organization/test-5678"), transformed.organization)
        assertEquals(listOf(CodeableConcept(text = "code")), transformed.code)
        assertEquals(listOf(CodeableConcept(text = "specialty")), transformed.specialty)
        assertEquals(listOf(Reference(reference = "Location/test-9012")), transformed.location)
        assertEquals(
            listOf(Reference(reference = "HealthcareService/test-3456")),
            transformed.healthcareService
        )
        assertEquals(
            listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
            transformed.telecom
        )
        assertEquals(listOf(AvailableTime(allDay = false)), transformed.availableTime)
        assertEquals(listOf(NotAvailable(description = "Not available now")), transformed.notAvailable)
        assertEquals("exceptions", transformed.availabilityExceptions)
        assertEquals(listOf(Reference(reference = "Endpoint/test-1357")), transformed.endpoint)
    }

    @Test
    fun `transforms practitioner role with only required attributes`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            practitioner = Reference(reference = "Practitioner/1234"),
            organization = Reference(reference = "Organization/5678")
        )

        val transformed = OncologyPractitionerRole.transform(practitionerRole, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertNull(transformed.meta)
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test"
                )
            ),
            transformed.identifier
        )
        assertNull(transformed.active)
        assertNull(transformed.period)
        assertEquals(Reference(reference = "Practitioner/test-1234"), transformed.practitioner)
        assertEquals(Reference(reference = "Organization/test-5678"), transformed.organization)
        assertEquals(listOf<CodeableConcept>(), transformed.code)
        assertEquals(listOf<CodeableConcept>(), transformed.specialty)
        assertEquals(listOf<Reference>(), transformed.location)
        assertEquals(listOf<Reference>(), transformed.healthcareService)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertEquals(listOf<AvailableTime>(), transformed.availableTime)
        assertEquals(listOf<NotAvailable>(), transformed.notAvailable)
        assertNull(transformed.availabilityExceptions)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }

    @Test
    fun `transforms practitioner role with all telecoms filtered`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            practitioner = Reference(reference = "Practitioner/1234"),
            organization = Reference(reference = "Organization/5678"),
            telecom = listOf(ContactPoint(id = "first"), ContactPoint(id = "second"))
        )

        val transformed = OncologyPractitionerRole.transform(practitionerRole, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertNull(transformed.meta)
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test"
                )
            ),
            transformed.identifier
        )
        assertNull(transformed.active)
        assertNull(transformed.period)
        assertEquals(Reference(reference = "Practitioner/test-1234"), transformed.practitioner)
        assertEquals(Reference(reference = "Organization/test-5678"), transformed.organization)
        assertEquals(listOf<CodeableConcept>(), transformed.code)
        assertEquals(listOf<CodeableConcept>(), transformed.specialty)
        assertEquals(listOf<Reference>(), transformed.location)
        assertEquals(listOf<Reference>(), transformed.healthcareService)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertEquals(listOf<AvailableTime>(), transformed.availableTime)
        assertEquals(listOf<NotAvailable>(), transformed.notAvailable)
        assertNull(transformed.availabilityExceptions)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }

    @Test
    fun `transforms practitioner role with some telecoms filtered`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            practitioner = Reference(reference = "Practitioner/1234"),
            organization = Reference(reference = "Organization/5678"),
            telecom = listOf(
                ContactPoint(system = ContactPointSystem.PHONE, value = "8675309"),
                ContactPoint(id = "second"),
                ContactPoint(id = "third"),
                ContactPoint(system = ContactPointSystem.EMAIL, value = "doctor@hospital.org")
            )
        )

        val transformed = OncologyPractitionerRole.transform(practitionerRole, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertNull(transformed.meta)
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test"
                )
            ),
            transformed.identifier
        )
        assertNull(transformed.active)
        assertNull(transformed.period)
        assertEquals(Reference(reference = "Practitioner/test-1234"), transformed.practitioner)
        assertEquals(Reference(reference = "Organization/test-5678"), transformed.organization)
        assertEquals(listOf<CodeableConcept>(), transformed.code)
        assertEquals(listOf<CodeableConcept>(), transformed.specialty)
        assertEquals(listOf<Reference>(), transformed.location)
        assertEquals(listOf<Reference>(), transformed.healthcareService)
        assertEquals(
            listOf(
                ContactPoint(system = ContactPointSystem.PHONE, value = "8675309"),
                ContactPoint(system = ContactPointSystem.EMAIL, value = "doctor@hospital.org")
            ),
            transformed.telecom
        )
        assertEquals(listOf<AvailableTime>(), transformed.availableTime)
        assertEquals(listOf<NotAvailable>(), transformed.notAvailable)
        assertNull(transformed.availabilityExceptions)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }
}
