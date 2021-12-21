package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class Base64BinaryTest {
    @Test
    fun `accepts valid value`() {
        val base64Binary = Base64Binary("abcdefgh")
        assertEquals("abcdefgh", base64Binary.value)
    }

    @Test
    fun `accepts leading spaces`() {
        val base64Binary = Base64Binary("  abcd")
        assertEquals("  abcd", base64Binary.value)
    }

    @Test
    fun `accepts trailing spaces`() {
        val base64Binary = Base64Binary("abcd  ")
        assertEquals("abcd  ", base64Binary.value)
    }

    @Test
    fun `fails on value with wrong number of values`() {
        val exception = assertThrows<IllegalArgumentException> { Base64Binary("abc") }
        assertEquals("Supplied value is not valid for a Base64Binary", exception.message)
    }

    @Test
    fun `can serialize`() {
        val base64Binary = Base64Binary("abcdefgh")
        val json = objectMapper.writeValueAsString(base64Binary)
        assertEquals("\"abcdefgh\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"abcdefgh\""
        val base64Binary = objectMapper.readValue<Base64Binary>(json)
        assertEquals(Base64Binary("abcdefgh"), base64Binary)
    }
}
