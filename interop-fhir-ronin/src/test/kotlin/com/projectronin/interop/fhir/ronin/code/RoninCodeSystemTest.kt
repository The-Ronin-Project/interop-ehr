package com.projectronin.interop.fhir.ronin.code

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class RoninCodeSystemTest {
    @Test
    fun `codecov for enum values`() {
        RoninCodeSystem.values().forEach { enum ->
            assertNotNull(RoninCodeSystem.values().firstOrNull { it.uri.value == enum.uri.value })
        }
    }
}
