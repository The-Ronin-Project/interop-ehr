package com.projectronin.interop.fhir.ronin.profile

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class RoninProfileTest {
    @Test
    fun `codecov for enum values`() {
        RoninProfile.values().forEach { enum ->
            assertNotNull(RoninProfile.values().firstOrNull { it.value == enum.value })
        }
    }
}
