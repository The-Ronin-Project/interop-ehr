package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.HttpVerb
import com.projectronin.interop.fhir.r4.valueset.SearchEntryMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BundleEntryTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val bundleEntry = BundleEntry(
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
            resource = Patient(id = Id("1234")),
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
            |    "resourceType" : "Patient",
            |    "id" : "1234"
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

        val deserializedBundleEntry = objectMapper.readValue<BundleEntry>(json)
        assertEquals(bundleEntry, deserializedBundleEntry)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val bundleEntry = BundleEntry(search = BundleSearch(mode = SearchEntryMode.INCLUDE))

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
        val bundleEntry = objectMapper.readValue<BundleEntry>(json)

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
