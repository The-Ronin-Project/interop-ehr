package com.projectronin.interop.fhir.epic

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LineValueTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val lineValue = LineValue(
            lineNumber = 1,
            subLines = listOf(
                SubLine(subLineNumber = 1, value = "abc")
            ),
            value = "value"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineValue)

        val expectedJson = """
            {
              "LineNumber" : 1,
              "SubLines" : [ {
                "SubLineNumber" : 1,
                "Value" : "abc"
              } ],
              "Value" : "value"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedLineValue = JacksonManager.objectMapper.readValue<LineValue>(json)
        assertEquals(lineValue, deserializedLineValue)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val lineValue = LineValue(
            lineNumber = 1,
            subLines = listOf(),
            value = "value"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineValue)

        val expectedJson = """
            {
              "LineNumber" : 1,
              "Value" : "value"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
              "LineNumber" : 1,
              "Value" : "value"
            }
        """.trimIndent()
        val lineValue = JacksonManager.objectMapper.readValue<LineValue>(json)

        assertEquals(listOf<SubLine>(), lineValue.subLines)
    }
}
