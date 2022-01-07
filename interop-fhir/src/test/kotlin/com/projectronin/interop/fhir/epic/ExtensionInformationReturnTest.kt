package com.projectronin.interop.fhir.epic

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExtensionInformationReturnTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val extensionInformationReturn = ExtensionInformationReturn(
            extensionIds = listOf(
                IdType(id = "12345", type = "type")
            ),
            extensionName = "extension name",
            lines = listOf(
                LineValue(
                    lineNumber = 1,
                    subLines = listOf(),
                    value = "value"
                )
            ),
            value = "value"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(extensionInformationReturn)

        val expectedJson = """
            {
              "ExtensionIds" : [ {
                "ID" : "12345",
                "Type" : "type"
              } ],
              "ExtensionName" : "extension name",
              "Lines" : [ {
                "LineNumber" : 1,
                "Value" : "value"
              } ],
              "Value" : "value"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedExtensionInformationReturn = JacksonManager.objectMapper.readValue<ExtensionInformationReturn>(json)
        assertEquals(extensionInformationReturn, deserializedExtensionInformationReturn)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val extensionInformationReturn = ExtensionInformationReturn(
            extensionIds = listOf(),
            extensionName = "extension name",
            lines = listOf(),
            value = "value"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(extensionInformationReturn)

        val expectedJson = """
            {
              "ExtensionName" : "extension name",
              "Value" : "value"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
              "ExtensionName" : "extension name",
              "Value" : "value"
            }
        """.trimIndent()
        val extensionInformationReturn = JacksonManager.objectMapper.readValue<ExtensionInformationReturn>(json)

        assertEquals(listOf<IdType>(), extensionInformationReturn.extensionIds)
        assertEquals(listOf<LineValue>(), extensionInformationReturn.lines)
    }
}
