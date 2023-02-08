package com.projectronin.interop.ehr.cerner.exception

import com.projectronin.interop.common.logmarkers.LogMarkers
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResourceCreateExceptionTest {
    val tenant: Tenant = mockk() {
        every { mnemonic } returns "test"
    }

    @Test
    fun `creates the proper message`() {
        val exception = ResourceCreateException(tenant, "/Resource") { "My message" }
        assertEquals("Exception when calling /Resource for test: My message", exception.message)
    }

    @Test
    fun `returns a HttpRequestFailure marker`() {
        val exception = ResourceCreateException(tenant, "/Resource") { "My message" }
        assertEquals(LogMarkers.HTTP_REQUEST_FAILURE, exception.logMarker)
    }
}
