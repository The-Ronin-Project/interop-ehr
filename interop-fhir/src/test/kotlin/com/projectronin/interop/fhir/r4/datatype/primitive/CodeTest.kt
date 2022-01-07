package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CodeTest {
    @Test
    fun `accepts valid value`() {
        val code = Code("Some Value")
        assertNotNull(code)
    }

    @Test
    fun `fails on empty value`() {
        val exception = assertThrows<IllegalArgumentException> { Code("") }
        assertEquals("Supplied value is not valid for a Code", exception.message)
    }

    @Test
    fun `fails on leading whitespace`() {
        val exception = assertThrows<IllegalArgumentException> { Code(" Value") }
        assertEquals("Supplied value is not valid for a Code", exception.message)
    }

    @Test
    fun `fails on consecutive internal whitespace`() {
        val exception = assertThrows<IllegalArgumentException> { Code("Some  Value") }
        assertEquals("Supplied value is not valid for a Code", exception.message)
    }

    @Test
    fun `fails on trailing whitespace`() {
        val exception = assertThrows<IllegalArgumentException> { Code("Value ") }
        assertEquals("Supplied value is not valid for a Code", exception.message)
    }

    @Test
    fun `can serialize`() {
        val code = Code("Some Value")
        val json = objectMapper.writeValueAsString(code)
        assertEquals("\"Some Value\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"Some Value\""
        val code = objectMapper.readValue<Code>(json)
        assertEquals(Code("Some Value"), code)
    }
}
