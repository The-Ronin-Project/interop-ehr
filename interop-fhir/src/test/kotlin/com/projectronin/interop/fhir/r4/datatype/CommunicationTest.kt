package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CommunicationTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val communication = Communication(
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
            language = CodeableConcept(text = "English"),
            preferred = true
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(communication)

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
              "language" : {
                "text" : "English"
              },
              "preferred" : true
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedCommunication = JacksonManager.objectMapper.readValue<Communication>(json)
        assertEquals(communication, deserializedCommunication)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val communication = Communication(language = CodeableConcept(text = "English"))
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(communication)

        val expectedJson = """
            {
              "language" : {
                "text" : "English"
              }
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedCommunication = JacksonManager.objectMapper.readValue<Communication>(json)
        assertEquals(communication, deserializedCommunication)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
              "language" : {
                "text" : "English"
              }
            }
        """.trimIndent()
        val communication = JacksonManager.objectMapper.readValue<Communication>(json)

        assertNull(communication.id)
        assertEquals(listOf<Extension>(), communication.extension)
        assertEquals(listOf<Extension>(), communication.modifierExtension)
        assertNull(communication.preferred)
    }
}
