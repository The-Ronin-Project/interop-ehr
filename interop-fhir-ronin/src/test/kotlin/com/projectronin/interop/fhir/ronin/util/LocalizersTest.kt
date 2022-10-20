package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocalizersTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `prepends tenant mnemonic to string`() {
        assertEquals("test-some string", "some string".localize(tenant))
    }

    @Test
    fun `non-reference String is not localized`() {
        assertEquals("http://espn.com", "http://espn.com".localizeReference(tenant))
    }

    @Test
    fun `localizes reference String with full URL and no history`() {
        assertEquals(
            "StructureDefinition/test-c8973a22-2b5b-4e76-9c66-00639c99e61b",
            "http://fhir.hl7.org/svc/StructureDefinition/c8973a22-2b5b-4e76-9c66-00639c99e61b".localizeReference(tenant)
        )
    }

    @Test
    fun `localizes reference String with full URL and history`() {
        assertEquals(
            "Observation/test-1x2/_history/2",
            "http://example.org/fhir/Observation/1x2/_history/2".localizeReference(tenant)
        )
    }

    @Test
    fun `localizes reference String with relative URL and no history`() {
        assertEquals("Patient/test-034AB16", "Patient/034AB16".localizeReference(tenant))
    }

    @Test
    fun `localizes reference String with relative URL and history`() {
        assertEquals("Patient/test-034AB16/_history/100", "Patient/034AB16/_history/100".localizeReference(tenant))
    }
}
