package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Practitioner
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Qualification
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.fhir.r4.resource.Practitioner as R4Practitioner

class R4PractitionerTransformerTest {
    private val transformer = R4PractitionerTransformer()
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `non R4 practitioner`() {
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformPractitioner(practitioner, tenant)
        }

        assertEquals("Practitioner is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `fails for practitioner with no ID`() {
        val r4Practitioner = R4Practitioner()
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Practitioner
        }

        val oncologyPractitioner = transformer.transformPractitioner(practitioner, tenant)
        assertNull(oncologyPractitioner)
    }

    @Test
    fun `fails for practitioner with no name`() {
        val r4Practitioner = R4Practitioner(
            id = Id("12345")
        )
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Practitioner
        }

        val oncologyPractitioner = transformer.transformPractitioner(practitioner, tenant)
        assertNull(oncologyPractitioner)
    }

    @Test
    fun `fails for practitioner name with no family name`() {
        val r4Practitioner = R4Practitioner(
            id = Id("12345"),
            name = listOf(HumanName(given = listOf("Jane")))
        )
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Practitioner
        }

        val oncologyPractitioner = transformer.transformPractitioner(practitioner, tenant)
        assertNull(oncologyPractitioner)
    }

    @Test
    fun `transforms practitioner with all attributes`() {
        val r4Practitioner = R4Practitioner(
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
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(ContactPoint(value = "8675309")),
            address = listOf(Address(country = "USA")),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            qualification = listOf(Qualification(code = CodeableConcept(text = "code"))),
            communication = listOf(CodeableConcept(text = "communication"))
        )
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Practitioner
        }

        val oncologyPractitioner = transformer.transformPractitioner(practitioner, tenant)

        oncologyPractitioner!! // Force it to be treated as non-null
        assertEquals("Practitioner", oncologyPractitioner.resourceType)
        assertEquals(Id("test-12345"), oncologyPractitioner.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner"))),
            oncologyPractitioner.meta
        )
        assertEquals(Uri("implicit-rules"), oncologyPractitioner.implicitRules)
        assertEquals(Code("en-US"), oncologyPractitioner.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), oncologyPractitioner.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            oncologyPractitioner.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyPractitioner.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyPractitioner.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyPractitioner.identifier
        )
        assertEquals(true, oncologyPractitioner.active)
        assertEquals(listOf(HumanName(family = "Doe")), oncologyPractitioner.name)
        assertEquals(listOf(ContactPoint(value = "8675309")), oncologyPractitioner.telecom)
        assertEquals(listOf(Address(country = "USA")), oncologyPractitioner.address)
        assertEquals(AdministrativeGender.FEMALE, oncologyPractitioner.gender)
        assertEquals(Date("1975-07-05"), oncologyPractitioner.birthDate)
        assertEquals(
            listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            oncologyPractitioner.photo
        )
        assertEquals(listOf(Qualification(code = CodeableConcept(text = "code"))), oncologyPractitioner.qualification)
        assertEquals(listOf(CodeableConcept(text = "communication")), oncologyPractitioner.communication)
    }

    @Test
    fun `transforms practitioner with only required attributes`() {
        val r4Practitioner = R4Practitioner(
            id = Id("12345"),
            name = listOf(HumanName(family = "Doe"))
        )
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Practitioner
        }

        val oncologyPractitioner = transformer.transformPractitioner(practitioner, tenant)

        oncologyPractitioner!! // Force it to be treated as non-null
        assertEquals("Practitioner", oncologyPractitioner.resourceType)
        assertEquals(Id("test-12345"), oncologyPractitioner.id)
        assertNull(oncologyPractitioner.meta)
        assertNull(oncologyPractitioner.implicitRules)
        assertNull(oncologyPractitioner.language)
        assertNull(oncologyPractitioner.text)
        assertEquals(listOf<ContainedResource>(), oncologyPractitioner.contained)
        assertEquals(listOf<Extension>(), oncologyPractitioner.extension)
        assertEquals(listOf<Extension>(), oncologyPractitioner.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyPractitioner.identifier
        )
        assertNull(oncologyPractitioner.active)
        assertEquals(listOf(HumanName(family = "Doe")), oncologyPractitioner.name)
        assertEquals(listOf<ContactPoint>(), oncologyPractitioner.telecom)
        assertEquals(listOf<Address>(), oncologyPractitioner.address)
        assertNull(oncologyPractitioner.gender)
        assertNull(oncologyPractitioner.birthDate)
        assertEquals(listOf<Attachment>(), oncologyPractitioner.photo)
        assertEquals(listOf<Qualification>(), oncologyPractitioner.qualification)
        assertEquals(listOf<CodeableConcept>(), oncologyPractitioner.communication)
    }

    @Test
    fun `non R4 bundle`() {
        val bundle = mockk<Bundle<Practitioner>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformPractitioners(bundle, tenant)
        }

        assertEquals("Bundle is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `bundle transformation returns empty when no valid transformations`() {
        val invalidPractitioner = R4Practitioner(
            name = listOf(HumanName(family = "Doe"))
        )

        val practitioner1 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidPractitioner
        }
        val practitioner2 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidPractitioner
        }

        val bundle = mockk<Bundle<Practitioner>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(practitioner1, practitioner2)
        }

        val oncologyPractitioners = transformer.transformPractitioners(bundle, tenant)
        assertEquals(0, oncologyPractitioners.size)
    }

    @Test
    fun `bundle transformation returns only valid transformations`() {
        val invalidPractitioner = R4Practitioner(
            name = listOf(HumanName(family = "Doe"))
        )
        val practitioner1 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidPractitioner
        }

        val r4Practitioner = R4Practitioner(
            id = Id("12345"),
            name = listOf(HumanName(family = "Doe"))
        )
        val practitioner2 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Practitioner
        }

        val bundle = mockk<Bundle<Practitioner>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(practitioner1, practitioner2)
        }

        val oncologyPractitioners = transformer.transformPractitioners(bundle, tenant)
        assertEquals(1, oncologyPractitioners.size)
    }

    @Test
    fun `bundle transformation returns all when all valid`() {
        val r4Practitioner = R4Practitioner(
            id = Id("12345"),
            name = listOf(HumanName(family = "Doe"))
        )
        val practitioner1 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Practitioner
        }

        val practitioner2 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Practitioner
        }

        val bundle = mockk<Bundle<Practitioner>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(practitioner1, practitioner2)
        }

        val oncologyPractitioners = transformer.transformPractitioners(bundle, tenant)
        assertEquals(2, oncologyPractitioners.size)
    }
}
