package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UrlTest {
    @Test
    fun `accepts valid value`() {
        val url = Url("http://localhost")
        assertNotNull(url)
    }

    @Test
    fun `fails on value with space`() {
        val exception = assertThrows<IllegalArgumentException> { Url("http://local host") }
        assertEquals("Supplied value is not valid for an Url", exception.message)
    }

    @Test
    fun `can serialize`() {
        val url = Url("http://localhost")
        val json = objectMapper.writeValueAsString(url)
        assertEquals("\"http://localhost\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"http://localhost\""
        val url = objectMapper.readValue<Url>(json)
        assertEquals(Url("http://localhost"), url)
    }
}
