package com.projectronin.interop.ehr.util

import com.projectronin.interop.common.exceptions.ServiceUnavailableException
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HandleHttpStatusErrorTest {
    private val service = "Service"
    private val tenantMnemonic = "Tenant"

    // Fake status code for testing
    private val HttpStatusCode.Companion.IAmATeaPot get() = HttpStatusCode(418, "I'm a tea pot")

    @Test
    fun `service unavailable test works`() {
        val exception = assertThrows<ServiceUnavailableException> {
            HttpStatusCode.ServiceUnavailable.handleErrorStatus(service, tenantMnemonic)
        }

        assertEquals("$service: Service unavailable for $tenantMnemonic", exception.message)
    }

    @Test
    fun `unknown test works`() {
        val exception = assertThrows<IOException> {
            HttpStatusCode.IAmATeaPot.handleErrorStatus(service, tenantMnemonic)
        }

        assertEquals(
            "Call to tenant $tenantMnemonic for $service failed with a ${HttpStatusCode.IAmATeaPot.value} ${HttpStatusCode.IAmATeaPot.description}",
            exception.message
        )
    }
}
