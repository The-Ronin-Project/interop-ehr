package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IdTest {
    @Test
    fun `accepts valid value`() {
        val id = Id("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.")
        assertNotNull(id)
    }

    @Test
    fun `fails on underscore`() {
        val exception = assertThrows<IllegalArgumentException> { Id("app_o_snd-ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.") }
        assertEquals("Supplied value is not valid for an Id", exception.message)
    }

    @Test
    fun `fails on empty value`() {
        val exception = assertThrows<IllegalArgumentException> { Id("") }
        assertEquals("Supplied value is not valid for an Id", exception.message)
    }

    @Test
    fun `accepts value over 64 characters`() {
        val id = { Id("A".repeat(65)) }
        assertNotNull(id)
    }

    @Test
    fun `fails on invalid character`() {
        val exception = assertThrows<IllegalArgumentException> { Id("@") }
        assertEquals("Supplied value is not valid for an Id", exception.message)
    }

    @Test
    fun `can serialize`() {
        val id = Id("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.")
        val json = objectMapper.writeValueAsString(id)
        assertEquals("\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.\""
        val id = objectMapper.readValue<Id>(json)
        assertEquals(Id("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-."), id)
    }
}
