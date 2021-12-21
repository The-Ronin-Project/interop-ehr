package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PositiveIntTest {
    @Test
    fun `accepts positive`() {
        val positiveInt = PositiveInt(1)
        assertNotNull(positiveInt)
    }

    @Test
    fun `fails on zero`() {
        val exception = assertThrows<IllegalArgumentException> { PositiveInt(0) }
        assertEquals("Supplied value is not valid for a PositiveInt", exception.message)
    }

    @Test
    fun `fails on negative`() {
        val exception = assertThrows<IllegalArgumentException> { PositiveInt(-1) }
        assertEquals("Supplied value is not valid for a PositiveInt", exception.message)
    }

    @Test
    fun `can serialize`() {
        val positiveInt = PositiveInt(100)
        val json = objectMapper.writeValueAsString(positiveInt)
        assertEquals("100", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "100"
        val positiveInt = objectMapper.readValue<PositiveInt>(json)
        assertEquals(PositiveInt(100), positiveInt)
    }
}
