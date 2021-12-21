package com.projectronin.interop.tenant.config.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class BatchConfigTest {
    @Test
    fun `check getters`() {
        val config = BatchConfig(LocalTime.of(20, 0), LocalTime.of(6, 30))
        assertEquals(LocalTime.of(20, 0), config.availableStart)
        assertEquals(LocalTime.of(6, 30), config.availableEnd)
    }
}
