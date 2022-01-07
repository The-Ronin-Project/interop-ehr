package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DateTimeTest {
    @Test
    fun `accepts YYYY`() {
        val dateTime = DateTime("2021")
        assertNotNull(dateTime)
    }

    @Test
    fun `accepts YYYY-MM`() {
        val dateTime = DateTime("2021-11")
        assertNotNull(dateTime)
    }

    @Test
    fun `accepts YYYY-MM-DD`() {
        val dateTime = DateTime("2021-11-16")
        assertNotNull(dateTime)
    }

    @Test
    fun `accepts YYYY-MM-DDThh_mm_ss+zz_zz`() {
        val dateTime = DateTime("2021-11-16T20:30:40+02:00")
        assertNotNull(dateTime)
    }

    @Test
    fun `fails on invalid input`() {
        val exception = assertThrows<IllegalArgumentException> { DateTime("abc") }
        assertEquals("Supplied value is not valid for a DateTime", exception.message)
    }

    @Test
    fun `fails on 24_00 time`() {
        val exception = assertThrows<IllegalArgumentException> { DateTime("2021-11-16T24:00:00Z") }
        assertEquals("Supplied value is not valid for a DateTime", exception.message)
    }

    @Test
    fun `can serialize`() {
        val dateTime = DateTime("2021-11-16T20:30:40+02:00")
        val json = objectMapper.writeValueAsString(dateTime)
        assertEquals("\"2021-11-16T20:30:40+02:00\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"2021-11-16T20:30:40+02:00\""
        val dateTime = objectMapper.readValue<DateTime>(json)
        assertEquals(DateTime("2021-11-16T20:30:40+02:00"), dateTime)
    }
}
