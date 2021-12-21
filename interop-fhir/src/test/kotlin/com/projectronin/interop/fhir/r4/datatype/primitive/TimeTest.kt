package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TimeTest {
    @Test
    fun `accepts valid time`() {
        val time = Time("10:00:25")
        assertNotNull(time)
    }

    @Test
    fun `accepts valid time with microseconds`() {
        val time = Time("10:00:25.1234")
        assertNotNull(time)
    }

    @Test
    fun `fails on value without seconds`() {
        val exception = assertThrows<IllegalArgumentException> { Time("09:20") }
        assertEquals("Supplied value is not valid for a Time", exception.message)
    }

    @Test
    fun `fails on value without minutes`() {
        val exception = assertThrows<IllegalArgumentException> { Time("08") }
        assertEquals("Supplied value is not valid for a Time", exception.message)
    }

    @Test
    fun `fails on 2400`() {
        val exception = assertThrows<IllegalArgumentException> { Time("24:00:00") }
        assertEquals("Supplied value is not valid for a Time", exception.message)
    }

    @Test
    fun `can serialize`() {
        val time = Time("10:00:25.1234")
        val json = objectMapper.writeValueAsString(time)
        assertEquals("\"10:00:25.1234\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"10:00:25.1234\""
        val time = objectMapper.readValue<Time>(json)
        assertEquals(Time("10:00:25.1234"), time)
    }
}
