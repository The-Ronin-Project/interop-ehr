package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.LinkType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PatientLinkTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val link = PatientLink(
            id = "67890",
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
            other = Reference(display = "reference"),
            type = LinkType.REPLACES
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(link)

        val expectedJson = """
            {
              "id" : "67890",
              "extension" : [ {
                "url" : "http://localhost/extension",
                "valueString" : "Value"
              } ],
              "modifierExtension" : [ {
                "url" : "http://localhost/modifier-extension",
                "valueString" : "Value"
              } ],
              "other" : {
                "display" : "reference"
              },
              "type" : "replaces"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedLink = JacksonManager.objectMapper.readValue<PatientLink>(json)
        assertEquals(link, deserializedLink)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val link = PatientLink(
            other = Reference(),
            type = LinkType.REPLACES
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(link)

        val expectedJson = """
            {
              "other" : { },
              "type" : "replaces"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedLink = JacksonManager.objectMapper.readValue<PatientLink>(json)
        assertEquals(link, deserializedLink)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
              "other" : { },
              "type" : "replaces"
            }
        """.trimIndent()
        val link = JacksonManager.objectMapper.readValue<PatientLink>(json)

        assertNull(link.id)
        assertEquals(listOf<Extension>(), link.extension)
        assertEquals(listOf<Extension>(), link.modifierExtension)
    }
}
