package com.projectronin.interop.fhir.r4.ronin.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.BundleLink
import com.projectronin.interop.fhir.r4.datatype.BundleRequest
import com.projectronin.interop.fhir.r4.datatype.BundleResponse
import com.projectronin.interop.fhir.r4.datatype.BundleSearch
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitioner
import com.projectronin.interop.fhir.r4.valueset.HttpVerb
import com.projectronin.interop.fhir.r4.valueset.SearchEntryMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RoninBundleEntryTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val bundleEntry = RoninBundleEntry(
            id = "12345",
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
            link = listOf(BundleLink(relation = "next", url = Uri("http://www.example.com/next"))),
            resource = OncologyPractitioner(
                identifier = listOf(
                    Identifier(
                        type = CodeableConcepts.RONIN_TENANT,
                        system = CodeSystem.RONIN_TENANT.uri,
                        value = "mdaoc"
                    )
                ),
                name = listOf(HumanName(family = "Doe"))
            ),
            search = BundleSearch(mode = SearchEntryMode.INCLUDE),
            request = BundleRequest(method = HttpVerb.GET, url = Uri("http://www.example.com/get")),
            response = BundleResponse(status = "Ok")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleEntry)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "modifierExtension" : [ {
            |    "url" : "http://localhost/modifier-extension",
            |    "valueString" : "Value"
            |  } ],
            |  "link" : [ {
            |    "relation" : "next",
            |    "url" : "http://www.example.com/next"
            |  } ],
            |  "resource" : {
            |    "resourceType" : "Practitioner",
            |    "identifier" : [ {
            |      "type" : {
            |        "coding" : [ {
            |          "system" : "http://projectronin.com/id/tenantId",
            |          "code" : "TID",
            |          "display" : "Ronin-specified Tenant Identifier"
            |        } ],
            |        "text" : "Tenant ID"
            |      },
            |      "system" : "http://projectronin.com/id/tenantId",
            |      "value" : "mdaoc"
            |    } ],
            |    "name" : [ {
            |      "family" : "Doe"
            |    } ]
            |  },
            |  "search" : {
            |    "mode" : "include"
            |  },
            |  "request" : {
            |    "method" : "GET",
            |    "url" : "http://www.example.com/get"
            |  },
            |  "response" : {
            |    "status" : "Ok"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedRoninBundleEntry = objectMapper.readValue<RoninBundleEntry>(json)
        assertEquals(bundleEntry, deserializedRoninBundleEntry)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val bundleEntry = RoninBundleEntry(search = BundleSearch(mode = SearchEntryMode.INCLUDE))

        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleEntry)

        val expectedJson = """
            |{
            |  "search" : {
            |    "mode" : "include"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "id" : "1234"
            |}""".trimMargin()
        val bundleEntry = objectMapper.readValue<RoninBundleEntry>(json)

        assertEquals("1234", bundleEntry.id)
        assertEquals(listOf<Extension>(), bundleEntry.extension)
        assertEquals(listOf<Extension>(), bundleEntry.modifierExtension)
        assertEquals(listOf<BundleLink>(), bundleEntry.link)
        assertNull(bundleEntry.resource)
        assertNull(bundleEntry.search)
        assertNull(bundleEntry.request)
        assertNull(bundleEntry.response)
    }
}
