package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DateTest {
    @Test
    fun `accepts YYYY`() {
        val date = Date("2021")
        assertNotNull(date)
    }

    @Test
    fun `accepts YYYY-MM`() {
        val date = Date("2021-11")
        assertNotNull(date)
    }

    @Test
    fun `accepts YYYY-MM-DD`() {
        val date = Date("2021-11-16")
        assertNotNull(date)
    }

    @Test
    fun `fails on invalid input`() {
        val exception = assertThrows<IllegalArgumentException> { Date("abc") }
        assertEquals("Supplied value is not valid for a Date", exception.message)
    }

    @Test
    fun `can serialize`() {
        val date = Date("2021-11-16")
        val json = objectMapper.writeValueAsString(date)
        assertEquals("\"2021-11-16\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"2021-11-16\""
        val date = objectMapper.readValue<Date>(json)
        assertEquals(Date("2021-11-16"), date)
    }
}
