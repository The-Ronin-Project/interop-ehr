package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.auth.CernerAuthenticationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.tenant.config.TenantService
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CernerHealthCheckServiceTest {
    private lateinit var authenticationService: CernerAuthenticationService
    private lateinit var cernerClient: CernerClient
    private lateinit var healthCheckService: CernerHealthCheckService
    private lateinit var tenantService: TenantService
    private val tenant = mockk<Tenant>()

    @BeforeEach
    fun setup() {
        authenticationService = mockk()
        cernerClient = mockk()
        tenantService = mockk()
        healthCheckService = CernerHealthCheckService(authenticationService, cernerClient, tenantService)
    }

    @Test
    fun `authentication fails`() {
        every { authenticationService.getAuthentication(tenant, true) } throws Exception()
        assertFalse(healthCheckService.healthCheck(tenant))
    }

    @Test
    fun `patient service fails`() {
        every { authenticationService.getAuthentication(tenant, true) } returns mockk()
        coEvery {
            cernerClient.options(tenant, "/")
        } throws Exception()
        assertFalse(healthCheckService.healthCheck(tenant))
    }

    @Test
    fun `health check works`() {
        every { authenticationService.getAuthentication(tenant, true) } returns mockk()
        coEvery {
            cernerClient.options(tenant, "/")
        } returns mockk()
        assertTrue(healthCheckService.healthCheck(tenant))
    }

    @Test
    fun `monitored tenants health check works`() {
        every { tenantService.getMonitoredTenants() } returns listOf(tenant)
        every { healthCheckService.healthCheck(tenant) } returns true

        val result = healthCheckService.healthCheck()
        assertEquals(1, result.size)
        assertTrue(result[tenant]!!)
    }
}
