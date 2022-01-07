package com.projectronin.interop.fhir.r4.ronin.resource

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
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
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OncologyPractitionerRoleTest {
    @Test
    fun `fails if no tenant identifier provided`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPractitionerRole(
                    identifier = listOf(),
                    practitioner = Reference(reference = "Practitioner/1234"),
                    organization = Reference(reference = "Organization/1234")
                )
            }
        assertEquals("Tenant identifier is required", exception.message)
    }

    @Test
    fun `fails if tenant does not have tenant codeable concept`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPractitionerRole(
                    identifier = listOf(Identifier(system = CodeSystem.RONIN_TENANT.uri, type = CodeableConcepts.SER)),
                    practitioner = Reference(reference = "Practitioner/1234"),
                    organization = Reference(reference = "Organization/1234")
                )
            }
        assertEquals("Tenant identifier provided without proper CodeableConcept defined", exception.message)
    }

    @Test
    fun `fails if telecom missing system`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPractitionerRole(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        )
                    ),
                    practitioner = Reference(reference = "Practitioner/1234"),
                    organization = Reference(reference = "Organization/1234"),
                    telecom = listOf(ContactPoint(value = "8675309"))
                )
            }
        assertEquals("All telecoms must have a system and value", exception.message)
    }

    @Test
    fun `fails if telecom missing value`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPractitionerRole(
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
            }
        assertEquals("All telecoms must have a system and value", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val practitionerRole = OncologyPractitionerRole(
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
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(practitionerRole)

        val expectedJson = """
            |{
            |  "resourceType" : "OncologyPractitionerRole",
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
            |  "period" : {
            |    "end" : "2022"
            |  },
            |  "practitioner" : {
            |    "reference" : "Practitioner/1234"
            |  },
            |  "organization" : {
            |    "reference" : "Organization/5678"
            |  },
            |  "code" : [ {
            |    "text" : "code"
            |  } ],
            |  "specialty" : [ {
            |    "text" : "specialty"
            |  } ],
            |  "location" : [ {
            |    "reference" : "Location/9012"
            |  } ],
            |  "healthcareService" : [ {
            |    "reference" : "HealthcareService/3456"
            |  } ],
            |  "telecom" : [ {
            |    "system" : "phone",
            |    "value" : "8675309"
            |  } ],
            |  "availableTime" : [ {
            |    "allDay" : false
            |  } ],
            |  "notAvailable" : [ {
            |    "description" : "Not available now"
            |  } ],
            |  "availabilityExceptions" : "exceptions",
            |  "endpoint" : [ {
            |    "reference" : "Endpoint/1357"
            |  } ]
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedPractitionerRole = objectMapper.readValue<OncologyPractitionerRole>(json)
        assertEquals(practitionerRole, deserializedPractitionerRole)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val practitionerRole = OncologyPractitionerRole(
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
            practitioner = Reference(reference = "Practitioner/1234"),
            organization = Reference(reference = "Organization/1234")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(practitionerRole)

        val expectedJson = """
            |{
            |  "resourceType" : "OncologyPractitionerRole",
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
            |  "practitioner" : {
            |    "reference" : "Practitioner/1234"
            |  },
            |  "organization" : {
            |    "reference" : "Organization/1234"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "resourceType" : "OncologyPractitionerRole",
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
            |  "practitioner" : {
            |    "reference" : "Practitioner/1234"
            |  },
            |  "organization" : {
            |    "reference" : "Organization/1234"
            |  }
            |}""".trimMargin()
        val practitionerRole = objectMapper.readValue<OncologyPractitionerRole>(json)

        assertNull(practitionerRole.id)
        assertNull(practitionerRole.meta)
        assertNull(practitionerRole.implicitRules)
        assertNull(practitionerRole.language)
        assertNull(practitionerRole.text)
        assertEquals(listOf<ContainedResource>(), practitionerRole.contained)
        assertEquals(listOf<Extension>(), practitionerRole.extension)
        assertEquals(listOf<Extension>(), practitionerRole.modifierExtension)

        val expectedIdentifier = Identifier(
            type = CodeableConcepts.RONIN_TENANT,
            system = CodeSystem.RONIN_TENANT.uri,
            value = "mdaoc"
        )
        assertEquals(listOf(expectedIdentifier), practitionerRole.identifier)

        assertNull(practitionerRole.active)
        assertNull(practitionerRole.period)
        assertEquals(Reference(reference = "Practitioner/1234"), practitionerRole.practitioner)
        assertEquals(Reference(reference = "Organization/1234"), practitionerRole.organization)
        assertEquals(listOf<CodeableConcept>(), practitionerRole.code)
        assertEquals(listOf<CodeableConcept>(), practitionerRole.specialty)
        assertEquals(listOf<Reference>(), practitionerRole.location)
        assertEquals(listOf<Reference>(), practitionerRole.healthcareService)
        assertEquals(listOf<ContactPoint>(), practitionerRole.telecom)
        assertEquals(listOf<AvailableTime>(), practitionerRole.availableTime)
        assertEquals(listOf<NotAvailable>(), practitionerRole.notAvailable)
        assertNull(practitionerRole.availabilityExceptions)
        assertEquals(listOf<Reference>(), practitionerRole.endpoint)
    }
}
