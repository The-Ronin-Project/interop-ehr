package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UnsignedIntTest {
    @Test
    fun `accepts positive`() {
        val unsignedInt = UnsignedInt(1)
        assertNotNull(unsignedInt)
    }

    @Test
    fun `accepts zero`() {
        val unsignedInt = UnsignedInt(0)
        assertNotNull(unsignedInt)
    }

    @Test
    fun `fails on negative`() {
        val exception = assertThrows<IllegalArgumentException> { UnsignedInt(-1) }
        assertEquals("Supplied value is not valid for an UnsignedInt", exception.message)
    }

    @Test
    fun `can serialize`() {
        val unsignedInt = UnsignedInt(1)
        val json = objectMapper.writeValueAsString(unsignedInt)
        assertEquals("1", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "1"
        val unsignedInt = objectMapper.readValue<UnsignedInt>(json)
        assertEquals(UnsignedInt(1), unsignedInt)
    }
}
