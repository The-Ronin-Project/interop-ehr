package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.auth.EpicAuthenticationService
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EpicHealthCheckServiceTest {
    private lateinit var authenticationService: EpicAuthenticationService
    private lateinit var patientService: EpicPatientService
    private lateinit var healthCheckService: EpicHealthCheckService
    private val tenant = mockk<Tenant>()

    @BeforeEach
    fun setup() {
        authenticationService = mockk()
        patientService = mockk()
        healthCheckService = EpicHealthCheckService(authenticationService, patientService)
    }

    @Test
    fun `authentication fails`() {
        every { authenticationService.getAuthentication(tenant) } throws Exception()
        assertFalse(healthCheckService.healthCheck(tenant))
    }

    @Test
    fun `patient service fails`() {
        every { authenticationService.getAuthentication(tenant) } returns mockk()
        every { patientService.findPatient(tenant, any(), "Health", "Check") } throws Exception()
        assertFalse(healthCheckService.healthCheck(tenant))
    }

    @Test
    fun `health check works`() {
        every { authenticationService.getAuthentication(tenant) } returns mockk()
        every { patientService.findPatient(tenant, any(), "Health", "Check") } returns mockk()
        assertTrue(healthCheckService.healthCheck(tenant))
    }
}
