package com.projectronin.interop.ehr.model
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReferenceTest {
    @Test
    fun `enum works`() {
        assertEquals("Provider", Reference.ReferenceType.Provider.name)
    }
}
