package com.projectronin.interop.fhir.epic

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItemValueTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val itemValue = ItemValue(
            itemNumber = "12345",
            lines = listOf(
                LineValue(
                    lineNumber = 1,
                    subLines = listOf(),
                    value = "value"
                )
            ),
            value = "value"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(itemValue)

        val expectedJson = """
            {
              "ItemNumber" : "12345",
              "Lines" : [ {
                "LineNumber" : 1,
                "Value" : "value"
              } ],
              "Value" : "value"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedItemValue = JacksonManager.objectMapper.readValue<ItemValue>(json)
        assertEquals(itemValue, deserializedItemValue)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val itemValue = ItemValue(
            itemNumber = "12345",
            lines = listOf(),
            value = "value"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(itemValue)

        val expectedJson = """
            {
              "ItemNumber" : "12345",
              "Value" : "value"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
              "ItemNumber" : "12345",
              "Value" : "value"
            }
        """.trimIndent()
        val itemValue = JacksonManager.objectMapper.readValue<ItemValue>(json)

        assertEquals(listOf<LineValue>(), itemValue.lines)
    }
}
