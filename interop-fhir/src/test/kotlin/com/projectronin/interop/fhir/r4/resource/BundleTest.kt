package com.projectronin.interop.fhir.r4.resource

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.BundleEntry
import com.projectronin.interop.fhir.r4.datatype.BundleLink
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.Signature
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.BundleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BundleTest {
    @Test
    fun `can serialize and deserialize JSON with known resource type`() {
        val patient = Patient(id = Id("5678"), name = listOf(HumanName(family = "Doe", given = listOf("John"))))
        val bundle = Bundle(
            id = Id("1234"),
            meta = Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner"))),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            identifier = Identifier(value = "identifier"),
            type = BundleType.SEARCHSET,
            timestamp = Instant("2017-01-01T00:00:00Z"),
            total = UnsignedInt(1),
            link = listOf(BundleLink(relation = "next", url = Uri("http://example.com"))),
            entry = listOf(BundleEntry(resource = patient)),
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
                  "id" : "5678",
                  "name" : [ {
                    "family" : "Doe",
                    "given" : [ "John" ]
                  } ]
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

        val deserializedBundle = objectMapper.readValue<Bundle>(json)
        assertEquals(bundle, deserializedBundle)
    }

    @Test
    fun `can serialize and deserialize JSON with unknown resource type`() {
        val unknownResource = UnknownResource(
            resourceType = "Banana",
            id = Id("5678"),
            otherData = mapOf("key" to "value", "key2" to mapOf("value" to "sub value"))
        )
        val bundle = Bundle(
            id = Id("1234"),
            meta = Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner"))),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            identifier = Identifier(value = "identifier"),
            type = BundleType.SEARCHSET,
            timestamp = Instant("2017-01-01T00:00:00Z"),
            total = UnsignedInt(1),
            link = listOf(BundleLink(relation = "next", url = Uri("http://example.com"))),
            entry = listOf(BundleEntry(resource = unknownResource)),
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

        val deserializedBundle = objectMapper.readValue<Bundle>(json)
        assertEquals(bundle, deserializedBundle)
    }
}
