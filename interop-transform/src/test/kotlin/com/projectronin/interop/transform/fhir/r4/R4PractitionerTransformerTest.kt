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
        val practitionerJson = "{}"
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerJson
        }

        val oncologyPractitioner = transformer.transformPractitioner(practitioner, tenant)
        assertNull(oncologyPractitioner)
    }

    @Test
    fun `fails for practitioner with no name`() {
        val practitionerJson = """{
                |  "id" : "12345"
                |}""".trimMargin()
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerJson
        }

        val oncologyPractitioner = transformer.transformPractitioner(practitioner, tenant)
        assertNull(oncologyPractitioner)
    }

    @Test
    fun `fails for practitioner name with no family name`() {
        val practitionerJson = """{
                |  "id" : "12345",
                |  "name" : [ {
                |    "given" : [ "Jane" ]
                |  } ]
                |}""".trimMargin()
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerJson
        }

        val oncologyPractitioner = transformer.transformPractitioner(practitioner, tenant)
        assertNull(oncologyPractitioner)
    }

    @Test
    fun `transforms practitioner with all attributes`() {
        val practitionerJson = """
            |{
            |  "resourceType" : "Practitioner",
            |  "id" : "12345",
            |  "meta" : {
            |    "profile" : [ "http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner" ]
            |  },
            |  "implicitRules" : "implicit-rules",
            |  "language" : "en-US",
            |  "text" : {
            |    "status" : "generated",
            |    "div" : "div"
            |  },
            |  "contained" : [ {"resourceType":"Banana","id":"24680"} ],
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "modifierExtension" : [ {
            |    "url" : "http://localhost/modifier-extension",
            |    "valueString" : "Value"
            |  } ],
            |  "identifier" : [ {
            |    "value" : "id"
            |  } ],
            |  "active" : true,
            |  "name" : [ {
            |    "family" : "Doe"
            |  } ],
            |  "telecom" : [ {
            |    "value" : "8675309"
            |  } ],
            |  "address" : [ {
            |    "country" : "USA"
            |  } ],
            |  "gender" : "female",
            |  "birthDate" : "1975-07-05",
            |  "photo" : [ {
            |    "contentType" : "text",
            |    "data" : "abcd"
            |  } ],
            |  "qualification" : [ {
            |    "code" : {
            |      "text" : "code"
            |    }
            |  } ],
            |  "communication" : [ {
            |    "text" : "communication"
            |  } ]
            |}""".trimMargin()
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerJson
        }

        val oncologyPractitioner = transformer.transformPractitioner(practitioner, tenant)

        oncologyPractitioner!! // Force it to be treated as non-null
        assertEquals("OncologyPractitioner", oncologyPractitioner.resourceType)
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
        val practitionerJson = """
            |{
            |  "resourceType" : "Practitioner",
            |  "id" : "12345",
            |  "name" : [ {
            |    "family" : "Doe"
            |  } ]
            |}""".trimMargin()
        val practitioner = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerJson
        }

        val oncologyPractitioner = transformer.transformPractitioner(practitioner, tenant)

        oncologyPractitioner!! // Force it to be treated as non-null
        assertEquals("OncologyPractitioner", oncologyPractitioner.resourceType)
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
        val practitioner1 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }
        val practitioner2 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
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
        val practitioner1 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }
        val practitioner2Json = """
            |{
            |  "resourceType" : "Practitioner",
            |  "id" : "12345",
            |  "name" : [ {
            |    "family" : "Doe"
            |  } ]
            |}""".trimMargin()
        val practitioner2 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitioner2Json
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
        val practitioner1Json = """
            |{
            |  "resourceType" : "Practitioner",
            |  "id" : "67890",
            |  "name" : [ {
            |    "family" : "Buck"
            |  } ]
            |}""".trimMargin()
        val practitioner1 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitioner1Json
        }

        val practitioner2Json = """
            |{
            |  "resourceType" : "Practitioner",
            |  "id" : "12345",
            |  "name" : [ {
            |    "family" : "Doe"
            |  } ]
            |}""".trimMargin()
        val practitioner2 = mockk<Practitioner> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitioner2Json
        }

        val bundle = mockk<Bundle<Practitioner>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(practitioner1, practitioner2)
        }

        val oncologyPractitioners = transformer.transformPractitioners(bundle, tenant)
        assertEquals(2, oncologyPractitioners.size)
    }
}
