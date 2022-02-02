package com.projectronin.interop.fhir.r4.ronin.resource

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.BundleLink
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.Signature
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.ronin.datatype.RoninBundleEntry
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.BundleType
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninBundleTest {
    @Test
    fun `can serialize and deserialize JSON with known resource type`() {
        val oncologyPatient = OncologyPatient(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                ),
                Identifier(
                    system = CodeSystem.MRN.uri,
                    type = CodeableConcepts.MRN,
                    value = "MRN"
                ),
                Identifier(
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    type = CodeableConcepts.FHIR_STU3_ID,
                    value = "fhirId"
                )
            ),
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    value = "8675309",
                    use = ContactPointUse.MOBILE
                )
            ),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M")
        )
        val bundle = RoninBundle(
            id = Id("1234"),
            meta = Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner"))),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            identifier = Identifier(value = "identifier"),
            type = BundleType.SEARCHSET,
            timestamp = Instant("2017-01-01T00:00:00Z"),
            total = UnsignedInt(1),
            link = listOf(BundleLink(relation = "next", url = Uri("http://example.com"))),
            entry = listOf(RoninBundleEntry(resource = oncologyPatient)),
            signature = Signature(
                type = listOf(Coding(display = "type")),
                `when` = Instant("2017-01-01T00:00:00Z"),
                who = Reference(reference = "who")
            )
        )

        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundle)

        val expectedJson = """
            {
              "resourceType" : "Bundle",
              "id" : "1234",
              "meta" : {
                "profile" : [ "http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner" ]
              },
              "implicitRules" : "implicit-rules",
              "language" : "en-US",
              "identifier" : {
                "value" : "identifier"
              },
              "type" : "searchset",
              "timestamp" : "2017-01-01T00:00:00Z",
              "total" : 1,
              "link" : [ {
                "relation" : "next",
                "url" : "http://example.com"
              } ],
              "entry" : [ {
                "resource" : {
                  "resourceType" : "Patient",
                  "identifier" : [ {
                    "type" : {
                      "coding" : [ {
                        "system" : "http://projectronin.com/id/tenantId",
                        "code" : "TID",
                        "display" : "Ronin-specified Tenant Identifier"
                      } ],
                      "text" : "Tenant ID"
                    },
                    "system" : "http://projectronin.com/id/tenantId",
                    "value" : "tenantId"
                  }, {
                    "type" : {
                      "coding" : [ {
                        "system" : "http://projectronin.com/id/mrn",
                        "code" : "MR",
                        "display" : "Medical Record Number"
                      } ],
                      "text" : "MRN"
                    },
                    "system" : "http://projectronin.com/id/mrn",
                    "value" : "MRN"
                  }, {
                    "type" : {
                      "coding" : [ {
                        "system" : "http://projectronin.com/id/fhir",
                        "code" : "STU3",
                        "display" : "FHIR STU3 ID"
                      } ],
                      "text" : "FHIR STU3"
                    },
                    "system" : "http://projectronin.com/id/fhir",
                    "value" : "fhirId"
                  } ],
                  "name" : [ {
                    "family" : "Doe"
                  } ],
                  "telecom" : [ {
                    "system" : "phone",
                    "value" : "8675309",
                    "use" : "mobile"
                  } ],
                  "gender" : "female",
                  "birthDate" : "1975-07-05",
                  "address" : [ {
                    "country" : "USA"
                  } ],
                  "maritalStatus" : {
                    "text" : "M"
                  }
                }
              } ],
              "signature" : {
                "type" : [ {
                  "display" : "type"
                } ],
                "when" : "2017-01-01T00:00:00Z",
                "who" : {
                  "reference" : "who"
                }
              }
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedRoninBundle = objectMapper.readValue<RoninBundle>(json)
        assertEquals(bundle, deserializedRoninBundle)
    }

    @Test
    fun `can serialize and deserialize JSON with unknown resource type`() {
        val unknownResource = UnknownRoninResource(
            resourceType = "Banana",
            id = Id("5678"),
            otherData = mapOf("key" to "value", "key2" to mapOf("value" to "sub value"))
        )
        val bundle = RoninBundle(
            id = Id("1234"),
            meta = Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner"))),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            identifier = Identifier(value = "identifier"),
            type = BundleType.SEARCHSET,
            timestamp = Instant("2017-01-01T00:00:00Z"),
            total = UnsignedInt(1),
            link = listOf(BundleLink(relation = "next", url = Uri("http://example.com"))),
            entry = listOf(RoninBundleEntry(resource = unknownResource)),
            signature = Signature(
                type = listOf(Coding(display = "type")),
                `when` = Instant("2017-01-01T00:00:00Z"),
                who = Reference(reference = "who")
            )
        )

        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundle)

        val expectedJson = """
            {
              "resourceType" : "Bundle",
              "id" : "1234",
              "meta" : {
                "profile" : [ "http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner" ]
              },
              "implicitRules" : "implicit-rules",
              "language" : "en-US",
              "identifier" : {
                "value" : "identifier"
              },
              "type" : "searchset",
              "timestamp" : "2017-01-01T00:00:00Z",
              "total" : 1,
              "link" : [ {
                "relation" : "next",
                "url" : "http://example.com"
              } ],
              "entry" : [ {
                "resource" : {
                  "resourceType" : "Banana",
                  "id" : "5678",
                  "key" : "value",
                  "key2" : {
                    "value" : "sub value"
                  }
                }
              } ],
              "signature" : {
                "type" : [ {
                  "display" : "type"
                } ],
                "when" : "2017-01-01T00:00:00Z",
                "who" : {
                  "reference" : "who"
                }
              }
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedRoninBundle = objectMapper.readValue<RoninBundle>(json)
        assertEquals(bundle, deserializedRoninBundle)
    }
}
