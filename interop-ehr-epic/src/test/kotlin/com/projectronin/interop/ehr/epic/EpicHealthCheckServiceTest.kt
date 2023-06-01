package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.auth.EpicAuthenticationService
import com.projectronin.interop.tenant.config.TenantService
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EpicHealthCheckServiceTest {
    private lateinit var authenticationService: EpicAuthenticationService
    private lateinit var patientService: EpicPatientService
    private lateinit var healthCheckService: EpicHealthCheckService
    private lateinit var tenantService: TenantService
    private val tenant = mockk<Tenant>()

    @BeforeEach
    fun setup() {
        authenticationService = mockk()
        patientService = mockk()
        tenantService = mockk()
        healthCheckService = EpicHealthCheckService(authenticationService, patientService, tenantService)
    }

    @Test
    fun `authentication fails`() {
        every { authenticationService.getAuthentication(tenant, true) } throws Exception()
        assertFalse(healthCheckService.healthCheck(tenant))
    }

    @Test
    fun `patient service fails`() {
        every { authenticationService.getAuthentication(tenant, true) } returns mockk()
        every { patientService.findPatient(tenant, any(), "Health", "Check", true) } throws Exception()
        assertFalse(healthCheckService.healthCheck(tenant))
    }

    @Test
    fun `health check works`() {
        every { authenticationService.getAuthentication(tenant, true) } returns mockk()
        every { patientService.findPatient(tenant, any(), "Health", "Check", true) } returns mockk()
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
