package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.HttpVerb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BundleRequestTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val bundleRequest = BundleRequest(
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
            method = HttpVerb.GET,
            url = Uri("http://www.example.com/get"),
            ifNoneMatch = "if none match",
            ifModifiedSince = Instant("2017-01-01T00:00:00Z"),
            ifMatch = "if match",
            ifNoneExist = "if none exist"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleRequest)

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
            |  "method" : "GET",
            |  "url" : "http://www.example.com/get",
            |  "ifNoneMatch" : "if none match",
            |  "ifModifiedSince" : "2017-01-01T00:00:00Z",
            |  "ifMatch" : "if match",
            |  "ifNoneExist" : "if none exist"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedBundleRequest = objectMapper.readValue<BundleRequest>(json)
        assertEquals(bundleRequest, deserializedBundleRequest)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val bundleRequest = BundleRequest(method = HttpVerb.POST, url = Uri("http://www.example.com/post"))

        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleRequest)

        val expectedJson = """
            |{
            |  "method" : "POST",
            |  "url" : "http://www.example.com/post"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "method" : "PUT",
            |  "url" : "http://www.example.com/put"
            |}""".trimMargin()
        val bundleRequest = objectMapper.readValue<BundleRequest>(json)

        assertNull(bundleRequest.id)
        assertEquals(listOf<Extension>(), bundleRequest.extension)
        assertEquals(listOf<Extension>(), bundleRequest.modifierExtension)
        assertEquals(HttpVerb.PUT, bundleRequest.method)
        assertEquals(Uri("http://www.example.com/put"), bundleRequest.url)
        assertNull(bundleRequest.ifNoneMatch)
        assertNull(bundleRequest.ifModifiedSince)
        assertNull(bundleRequest.ifMatch)
        assertNull(bundleRequest.ifNoneExist)
    }
}
