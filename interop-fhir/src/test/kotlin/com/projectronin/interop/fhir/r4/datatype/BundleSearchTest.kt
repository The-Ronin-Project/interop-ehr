package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.SearchEntryMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BundleSearchTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val bundleSearch = BundleSearch(
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
            mode = SearchEntryMode.INCLUDE,
            score = Decimal(1.4)
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleSearch)

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
            |  "mode" : "include",
            |  "score" : 1.4
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedBundleSearch = objectMapper.readValue<BundleSearch>(json)
        assertEquals(bundleSearch, deserializedBundleSearch)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val bundleSearch = BundleSearch(mode = SearchEntryMode.OUTCOME)

        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleSearch)

        val expectedJson = """
            |{
            |  "mode" : "outcome"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "score" : 3.14
            |}""".trimMargin()
        val bundleSearch = objectMapper.readValue<BundleSearch>(json)

        assertNull(bundleSearch.id)
        assertEquals(listOf<Extension>(), bundleSearch.extension)
        assertEquals(listOf<Extension>(), bundleSearch.modifierExtension)
        assertNull(bundleSearch.mode)
        assertEquals(Decimal(3.14), bundleSearch.score)
    }
}
