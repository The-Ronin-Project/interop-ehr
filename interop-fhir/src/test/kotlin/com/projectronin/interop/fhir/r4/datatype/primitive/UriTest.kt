package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UriTest {
    @Test
    fun `accepts valid value`() {
        val uri = Uri("urn:uuid:53fefa32-fcbb-4ff8-8a92-55ee120877b7")
        assertNotNull(uri)
    }

    @Test
    fun `fails on value with space`() {
        val exception = assertThrows<IllegalArgumentException> { Uri("Some value") }
        assertEquals("Supplied value is not valid for an Uri", exception.message)
    }

    @Test
    fun `can serialize`() {
        val uri = Uri("urn:uuid:53fefa32-fcbb-4ff8-8a92-55ee120877b7")
        val json = objectMapper.writeValueAsString(uri)
        assertEquals("\"urn:uuid:53fefa32-fcbb-4ff8-8a92-55ee120877b7\"", json)
    }

    @Test
    fun `can deserialize`() {
        val json = "\"urn:uuid:53fefa32-fcbb-4ff8-8a92-55ee120877b7\""
        val uri = objectMapper.readValue<Uri>(json)
        assertEquals(Uri("urn:uuid:53fefa32-fcbb-4ff8-8a92-55ee120877b7"), uri)
    }
}
