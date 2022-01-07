package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InstantTest {
    @Test
    fun `accepts valid value`() {
        val instant = Instant("2017-01-01T00:00:00Z")
        assertNotNull(instant)
    }

    @Test
    fun `accepts minus timezone`() {
        val instant = Instant("2017-01-01T00:00:00-02:00")
        assertNotNull(instant)
    }

    @Test
    fun `accepts plus timezone`() {
        val instant = Instant("2017-01-01T00:00:00+10:30")
        assertNotNull(instant)
    }

    @Test
    fun `accepts microseconds`() {
        val instant = Instant("2017-01-01T00:00:00.123Z")
        assertNotNull(instant)
    }

    @Test
    fun `accepts microseconds with timezone`() {
        val instant = Instant("2017-01-01T00:00:00.123+05:00")
        assertNotNull(instant)
    }

    @Test
    fun `fails on invalid format`() {
        val exception = assertThrows<IllegalArgumentException> { Instant("2017-01-01 00:00:00") }
        assertEquals("Supplied value is not valid for an Instant", exception.message)
    }

    @Test
    fun `can serialize`() {
        val instant = Instant("2017-01-01T00:00:00.123+05:00")
        val json = objectMapper.writeValueAsString(instant)
        assertEquals("\"2017-01-01T00:00:00.123+05:00\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"2017-01-01T00:00:00.123+05:00\""
        val instant = objectMapper.readValue<Instant>(json)
        assertEquals(Instant("2017-01-01T00:00:00.123+05:00"), instant)
    }
}
