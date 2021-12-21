package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UuidTest {
    @Test
    fun `accepts valid value`() {
        val uuid = Uuid("urn:uuid:12345678-90ab-cdef-1234-567890abcdef")
        assertNotNull(uuid)
    }

    @Test
    fun `fails on non-uuid URN`() {
        val exception = assertThrows<IllegalArgumentException> { Uuid("urn:oid:12345678-90ab-cdef-1234-567890abcdef") }
        assertEquals("Supplied value is not valid for an Uuid", exception.message)
    }

    @Test
    fun `fails on value with space`() {
        val exception =
            assertThrows<IllegalArgumentException> { Uuid("urn:uuid:12345678-90ab-cd ef-1234-567890abcdef") }
        assertEquals("Supplied value is not valid for an Uuid", exception.message)
    }

    @Test
    fun `fails on value with too few characters`() {
        val exception = assertThrows<IllegalArgumentException> { Uuid("urn:uuid:12345678-90ab-cdef-1234-567890abcd") }
        assertEquals("Supplied value is not valid for an Uuid", exception.message)
    }

    @Test
    fun `fails on value with invalid pattern`() {
        val exception = assertThrows<IllegalArgumentException> { Uuid("urn:uuid:12345678-90ab-cdef-12345678-90abcdef") }
        assertEquals("Supplied value is not valid for an Uuid", exception.message)
    }

    @Test
    fun `can serialize`() {
        val uuid = Uuid("urn:uuid:12345678-90ab-cdef-1234-567890abcdef")
        val json = objectMapper.writeValueAsString(uuid)
        assertEquals("\"urn:uuid:12345678-90ab-cdef-1234-567890abcdef\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"urn:uuid:12345678-90ab-cdef-1234-567890abcdef\""
        val uuid = objectMapper.readValue<Uuid>(json)
        assertEquals(Uuid("urn:uuid:12345678-90ab-cdef-1234-567890abcdef"), uuid)
    }
}
