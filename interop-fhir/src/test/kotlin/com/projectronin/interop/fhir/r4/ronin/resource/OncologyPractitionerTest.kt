package com.projectronin.interop.fhir.r4.ronin.resource

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OncologyPractitionerTest {
    @Test
    fun `fails if no tenant identifier provided`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPractitioner(
                    identifier = listOf(),
                    name = listOf(HumanName(family = "Smith"))
                )
            }
        assertEquals("Tenant identifier is required", exception.message)
    }

    @Test
    fun `fails if tenant does not have tenant codeable concept`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPractitioner(
                    identifier = listOf(Identifier(system = CodeSystem.RONIN_TENANT.uri, type = CodeableConcepts.SER)),
                    name = listOf(HumanName(family = "Smith"))
                )
            }
        assertEquals("Tenant identifier provided without proper CodeableConcept defined", exception.message)
    }

    @Test
    fun `fails if SER does not have SER codeable concept`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPractitioner(
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
            }
        assertEquals("SER provided without proper CodeableConcept defined", exception.message)
    }

    @Test
    fun `fails if no name provided`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPractitioner(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        )
                    ),
                    name = listOf()
                )
            }
        assertEquals("At least one name must be provided", exception.message)
    }

    @Test
    fun `fails if name provided without family`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPractitioner(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        )
                    ),
                    name = listOf(HumanName())
                )
            }
        assertEquals("All names must have a family name provided", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val practitioner = OncologyPractitioner(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner")),
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED,
                div = "div"
            ),
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
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
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
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(practitioner)

        val expectedJson = """
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
            |    "type" : {
            |      "coding" : [ {
            |        "system" : "http://projectronin.com/id/tenantId",
            |        "code" : "TID",
            |        "display" : "Ronin-specified Tenant Identifier"
            |      } ],
            |      "text" : "Tenant ID"
            |    },
            |    "system" : "http://projectronin.com/id/tenantId",
            |    "value" : "mdaoc"
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
        assertEquals(expectedJson, json)

        val deserializedPractitioner = objectMapper.readValue<OncologyPractitioner>(json)
        assertEquals(practitioner, deserializedPractitioner)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val practitioner = OncologyPractitioner(
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
            name = listOf(HumanName(family = "Doe"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(practitioner)

        val expectedJson = """
            |{
            |  "resourceType" : "Practitioner",
            |  "identifier" : [ {
            |    "type" : {
            |      "coding" : [ {
            |        "system" : "http://projectronin.com/id/tenantId",
            |        "code" : "TID",
            |        "display" : "Ronin-specified Tenant Identifier"
            |      } ],
            |      "text" : "Tenant ID"
            |    },
            |    "system" : "http://projectronin.com/id/tenantId",
            |    "value" : "mdaoc"
            |  } ],
            |  "name" : [ {
            |    "family" : "Doe"
            |  } ]
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "resourceType" : "OncologyPractitioner",
            |  "identifier" : [ {
            |    "type" : {
            |      "coding" : [ {
            |        "system" : "http://projectronin.com/id/tenantId",
            |        "code" : "TID",
            |        "display" : "Ronin-specified Tenant Identifier"
            |      } ],
            |      "text" : "Tenant ID"
            |    },
            |    "system" : "http://projectronin.com/id/tenantId",
            |    "value" : "mdaoc"
            |  } ],
            |  "name" : [ {
            |    "family" : "Doe"
            |  } ]
            |}""".trimMargin()
        val practitioner = objectMapper.readValue<OncologyPractitioner>(json)

        assertNull(practitioner.id)
        assertNull(practitioner.meta)
        assertNull(practitioner.implicitRules)
        assertNull(practitioner.language)
        assertNull(practitioner.text)
        assertEquals(listOf<ContainedResource>(), practitioner.contained)
        assertEquals(listOf<Extension>(), practitioner.extension)
        assertEquals(listOf<Extension>(), practitioner.modifierExtension)

        val expectedIdentifier = Identifier(
            type = CodeableConcepts.RONIN_TENANT,
            system = CodeSystem.RONIN_TENANT.uri,
            value = "mdaoc"
        )
        assertEquals(listOf(expectedIdentifier), practitioner.identifier)

        assertNull(practitioner.active)
        assertEquals(listOf(HumanName(family = "Doe")), practitioner.name)
        assertEquals(listOf<ContactPoint>(), practitioner.telecom)
        assertEquals(listOf<Address>(), practitioner.address)
        assertNull(practitioner.gender)
        assertNull(practitioner.birthDate)
        assertEquals(listOf<Attachment>(), practitioner.photo)
        assertEquals(listOf<Qualification>(), practitioner.qualification)
        assertEquals(listOf<CodeableConcept>(), practitioner.communication)
    }
}
