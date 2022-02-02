package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SubLineTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val subLine = SubLine(
            subLineNumber = 1,
            value = "abc"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(subLine)

        val expectedJson = """
            {
              "SubLineNumber" : 1,
              "Value" : "abc"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedSubLine = JacksonManager.objectMapper.readValue<SubLine>(json)
        assertEquals(subLine, deserializedSubLine)
    }
}
