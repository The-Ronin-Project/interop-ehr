package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExtensionTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val extension = Extension(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            url = Uri("http://hl7.org/fhir/StructureDefinition/iso-21090-EN-use"),
            value = DynamicValue(DynamicValueType.CODE, Code("I"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(extension)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "url" : "http://hl7.org/fhir/StructureDefinition/iso-21090-EN-use",
            |  "valueCode" : "I"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedExtension = objectMapper.readValue<Extension>(json)
        assertEquals(extension, deserializedExtension)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val extension = Extension(
            url = Uri("http://hl7.org/fhir/StructureDefinition/iso-21090-EN-use"),
            value = DynamicValue(DynamicValueType.CODE, Code("I"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(extension)

        val expectedJson = """
            |{
            |  "url" : "http://hl7.org/fhir/StructureDefinition/iso-21090-EN-use",
            |  "valueCode" : "I"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "url" : "http://hl7.org/fhir/StructureDefinition/iso-21090-EN-use",
            |  "valueCode" : "I"
            |}""".trimMargin()
        val extension = objectMapper.readValue<Extension>(json)

        assertNull(extension.id)
        assertEquals(listOf<Extension>(), extension.extension)
        assertEquals(Uri("http://hl7.org/fhir/StructureDefinition/iso-21090-EN-use"), extension.url)
        assertEquals(DynamicValue(DynamicValueType.CODE, Code("I")), extension.value)
    }
}
