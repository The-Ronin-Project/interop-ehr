package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OidTest {
    @Test
    fun `accepts valid value`() {
        val oid = Oid("urn:oid:1.2.3.4.5")
        assertNotNull(oid)
    }

    @Test
    fun `fails on non-oid URN`() {
        val exception = assertThrows<IllegalArgumentException> { Oid("urn:uuid:1.2.3.4.5") }
        assertEquals("Supplied value is not valid for an Oid", exception.message)
    }

    @Test
    fun `fails on value with space`() {
        val exception = assertThrows<IllegalArgumentException> { Oid("urn:oid:1.2 3.4.5") }
        assertEquals("Supplied value is not valid for an Oid", exception.message)
    }

    @Test
    fun `fails on value with leading invalid digit`() {
        val exception = assertThrows<IllegalArgumentException> { Oid("urn:oid:5.4.3.2.1") }
        assertEquals("Supplied value is not valid for an Oid", exception.message)
    }

    @Test
    fun `can serialize`() {
        val oid = Oid("urn:oid:1.2.3.4.5")
        val json = objectMapper.writeValueAsString(oid)
        assertEquals("\"urn:oid:1.2.3.4.5\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"urn:oid:1.2.3.4.5\""
        val oid = objectMapper.readValue<Oid>(json)
        assertEquals(Oid("urn:oid:1.2.3.4.5"), oid)
    }
}
