package com.projectronin.interop.ehr.inputs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FHIRSearchTokenTest {
    @Test
    fun `system with code inputs can be created`() {
        val token = FHIRSearchToken(system = "system", code = "value")
        assertEquals("system|value", token.toParam())
    }

    @Test
    fun `null system is accepted`() {
        val token = FHIRSearchToken(system = null, code = "value")
        assertEquals("value", token.toParam())
    }

    @Test
    fun `missing system becomes null`() {
        val token = FHIRSearchToken(code = "value")
        assertEquals("value", token.toParam())
    }

    @Test
    fun `empty system becomes null`() {
        val token = FHIRSearchToken(system = "", code = "value")
        assertEquals("value", token.toParam())
    }

    @Test
    fun `empty code throws exception`() {
        val exception = assertThrows<IllegalArgumentException> {
            FHIRSearchToken(system = "system", code = "")
        }
        assertEquals("A FHIR search token requires a code", exception.message)
    }
}
