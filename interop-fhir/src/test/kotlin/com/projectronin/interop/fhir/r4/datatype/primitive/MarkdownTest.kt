package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class MarkdownTest {
    @Test
    fun `accepts valid value`() {
        val markdown = Markdown("Some Value")
        assertNotNull(markdown)
    }

    // There are no currently invalid values for Markdown.

    @Test
    fun `can serialize`() {
        val markdown = Markdown("Some Value")
        val json = objectMapper.writeValueAsString(markdown)
        assertEquals("\"Some Value\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"Some Value\""
        val markdown = objectMapper.readValue<Markdown>(json)
        assertEquals(Markdown("Some Value"), markdown)
    }
}
