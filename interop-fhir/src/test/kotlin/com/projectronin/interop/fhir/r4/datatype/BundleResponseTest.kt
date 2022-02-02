package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BundleResponseTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val bundleResponse = BundleResponse(
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
            status = "Ok",
            location = Uri("http://www.example.com/location"),
            etag = "etag",
            lastModified = Instant("2015-02-07T13:28:17.239+02:00"),
            outcome = Patient(id = Id("67890"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleResponse)

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
            |  "status" : "Ok",
            |  "location" : "http://www.example.com/location",
            |  "etag" : "etag",
            |  "lastModified" : "2015-02-07T13:28:17.239+02:00",
            |  "outcome" : {
            |    "resourceType" : "Patient",
            |    "id" : "67890"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedBundleResponse = objectMapper.readValue<BundleResponse>(json)
        assertEquals(bundleResponse, deserializedBundleResponse)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val bundleResponse = BundleResponse(status = "status")

        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleResponse)

        val expectedJson = """
            |{
            |  "status" : "status"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "status" : "status"
            |}""".trimMargin()
        val bundleResponse = objectMapper.readValue<BundleResponse>(json)

        assertNull(bundleResponse.id)
        assertEquals(listOf<Extension>(), bundleResponse.extension)
        assertEquals(listOf<Extension>(), bundleResponse.modifierExtension)
        assertEquals("status", bundleResponse.status)
        assertNull(bundleResponse.location)
        assertNull(bundleResponse.etag)
        assertNull(bundleResponse.lastModified)
        assertNull(bundleResponse.outcome)
    }
}
