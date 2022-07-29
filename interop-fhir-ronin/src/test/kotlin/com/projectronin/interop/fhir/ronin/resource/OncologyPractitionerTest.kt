package com.projectronin.interop.fhir.ronin.resource

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
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OncologyPractitionerTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `validate fails if no tenant identifier provided`() {
        val practitioner = Practitioner(
            identifier = listOf(),
            name = listOf(HumanName(family = "Smith"))
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyPractitioner.validate(practitioner)
        }
        assertEquals("Tenant identifier is required", exception.message)
    }

    @Test
    fun `validate fails if tenant does not have tenant codeable concept`() {
        val practitioner = Practitioner(
            identifier = listOf(Identifier(system = CodeSystem.RONIN_TENANT.uri, type = CodeableConcepts.SER)),
            name = listOf(HumanName(family = "Smith"))
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyPractitioner.validate(practitioner)
        }
        assertEquals("Tenant identifier provided without proper CodeableConcept defined", exception.message)
    }

    @Test
    fun `validate fails if SER does not have SER codeable concept`() {
        val practitioner = Practitioner(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                ),
                Identifier(system = CodeSystem.SER.uri, type = CodeableConcepts.RONIN_TENANT)
            ),
            name = listOf(HumanName(family = "Smith"))
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyPractitioner.validate(practitioner)
        }
        assertEquals("SER provided without proper CodeableConcept defined", exception.message)
    }

    @Test
    fun `validate fails if no name provided`() {
        val practitioner = Practitioner(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            name = listOf()
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyPractitioner.validate(practitioner)
        }
        assertEquals("At least one name must be provided", exception.message)
    }

    @Test
    fun `validate fails if name provided without family`() {
        val practitioner = Practitioner(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            name = listOf(HumanName())
        )
        val exception = assertThrows<IllegalArgumentException> {
            OncologyPractitioner.validate(practitioner)
        }
        assertEquals("All names must have a family name provided", exception.message)
    }

    @Test
    fun `transform fails for practitioner with no ID`() {
        val practitioner = Practitioner()

        val transformed = OncologyPractitioner.transform(practitioner, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform fails for practitioner with no name`() {
        val practitioner = Practitioner(
            id = Id("12345")
        )

        val transformed = OncologyPractitioner.transform(practitioner, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform fails for practitioner name with no family name`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            name = listOf(HumanName(given = listOf("Jane")))
        )

        val transformed = OncologyPractitioner.transform(practitioner, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transforms practitioner with all attributes`() {
        val practitioner = Practitioner(
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
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
            address = listOf(Address(country = "USA")),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            qualification = listOf(Qualification(code = CodeableConcept(text = "code"))),
            communication = listOf(CodeableConcept(text = "communication"))
        )

        val transformed = OncologyPractitioner.transform(practitioner, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Practitioner", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
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
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test"),
                Identifier(type = CodeableConcepts.FHIR_STU3_ID, system = CodeSystem.FHIR_STU3_ID.uri, value = "12345")
            ),
            transformed.identifier
        )
        assertEquals(true, transformed.active)
        assertEquals(listOf(HumanName(family = "Doe")), transformed.name)
        assertEquals(
            listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
            transformed.telecom
        )
        assertEquals(listOf(Address(country = "USA")), transformed.address)
        assertEquals(AdministrativeGender.FEMALE, transformed.gender)
        assertEquals(Date("1975-07-05"), transformed.birthDate)
        assertEquals(
            listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            transformed.photo
        )
        assertEquals(listOf(Qualification(code = CodeableConcept(text = "code"))), transformed.qualification)
        assertEquals(listOf(CodeableConcept(text = "communication")), transformed.communication)
    }

    @Test
    fun `transforms practitioner with only required attributes`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            name = listOf(HumanName(family = "Doe"))
        )

        val transformed = OncologyPractitioner.transform(practitioner, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Practitioner", transformed.resourceType)
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
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test"),
                Identifier(type = CodeableConcepts.FHIR_STU3_ID, system = CodeSystem.FHIR_STU3_ID.uri, value = "12345")
            ),
            transformed.identifier
        )
        assertNull(transformed.active)
        assertEquals(listOf(HumanName(family = "Doe")), transformed.name)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertEquals(listOf<Address>(), transformed.address)
        assertNull(transformed.gender)
        assertNull(transformed.birthDate)
        assertEquals(listOf<Attachment>(), transformed.photo)
        assertEquals(listOf<Qualification>(), transformed.qualification)
        assertEquals(listOf<CodeableConcept>(), transformed.communication)
    }
}
