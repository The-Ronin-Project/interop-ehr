package com.projectronin.interop.tenant.config.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NoEHRFoundExceptionTest {
    @Test
    fun `requires message`() {
        val exception = NoEHRFoundException("message")
        assertEquals("message", exception.message)
        assertNull(exception.cause)
    }
}
