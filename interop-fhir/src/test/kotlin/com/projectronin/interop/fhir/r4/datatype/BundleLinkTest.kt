package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BundleLinkTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val bundleLink = BundleLink(
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
            relation = "next",
            url = Uri("http://www.example.com/next")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleLink)

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
            |  "relation" : "next",
            |  "url" : "http://www.example.com/next"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedBundleLink = objectMapper.readValue<BundleLink>(json)
        assertEquals(bundleLink, deserializedBundleLink)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val bundleLink = BundleLink(relation = "prev", url = Uri("http://www.example.com/prev"))

        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleLink)

        val expectedJson = """
            |{
            |  "relation" : "prev",
            |  "url" : "http://www.example.com/prev"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "relation" : "prev",
            |  "url" : "http://www.example.com/prev"
            |}""".trimMargin()
        val bundleLink = objectMapper.readValue<BundleLink>(json)

        assertNull(bundleLink.id)
        assertEquals(listOf<Extension>(), bundleLink.extension)
        assertEquals(listOf<Extension>(), bundleLink.modifierExtension)
        assertEquals("prev", bundleLink.relation)
        assertEquals(Uri("http://www.example.com/prev"), bundleLink.url)
    }
}
