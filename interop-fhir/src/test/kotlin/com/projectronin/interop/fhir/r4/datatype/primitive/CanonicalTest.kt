package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CanonicalTest {
    @Test
    fun `accepts valid value`() {
        val canonical = Canonical("urn:uuid:53fefa32-fcbb-4ff8-8a92-55ee120877b7")
        assertNotNull(canonical)
    }

    @Test
    fun `fails on value with space`() {
        val exception = assertThrows<IllegalArgumentException> { Canonical("Some value") }
        assertEquals("Supplied value is not valid for a Canonical", exception.message)
    }

    @Test
    fun `can serialize`() {
        val canonical = Canonical("urn:uuid:53fefa32-fcbb-4ff8-8a92-55ee120877b7")
        val json = objectMapper.writeValueAsString(canonical)
        assertEquals("\"urn:uuid:53fefa32-fcbb-4ff8-8a92-55ee120877b7\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"urn:uuid:53fefa32-fcbb-4ff8-8a92-55ee120877b7\""
        val canonical = objectMapper.readValue<Canonical>(json)
        assertEquals(Canonical("urn:uuid:53fefa32-fcbb-4ff8-8a92-55ee120877b7"), canonical)
    }
}
