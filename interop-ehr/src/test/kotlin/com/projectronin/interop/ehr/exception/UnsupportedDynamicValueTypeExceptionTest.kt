package com.projectronin.interop.ehr.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UnsupportedDynamicValueTypeExceptionTest {
    @Test
    fun `requires message`() {
        val exception = UnsupportedDynamicValueTypeException("message")
        assertEquals("message", exception.message)
        assertNull(exception.cause)
    }
}
