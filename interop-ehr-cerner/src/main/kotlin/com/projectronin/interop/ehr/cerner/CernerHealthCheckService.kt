package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.HealthCheckService
import com.projectronin.interop.ehr.cerner.auth.CernerAuthenticationService
import com.projectronin.interop.tenant.config.TenantService
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class CernerHealthCheckService(
    private val authorizationService: CernerAuthenticationService,
    private val patientService: CernerPatientService,
    private val tenantService: TenantService
) : HealthCheckService {
    private val logger = KotlinLogging.logger { }

    override fun healthCheck(tenant: Tenant): Boolean {
        try {
            authorizationService.getAuthentication(tenant)
        } catch (e: Exception) {
            logger.debug(e) { "Failed authorization health check for ${tenant.mnemonic}" }
            return false
        }
        try {
            patientService.findPatient(tenant, LocalDate.now(), "Health", "Check")
        } catch (e: Exception) {
            logger.debug(e) { "Failed Patient search health check for ${tenant.mnemonic}" }
            return false
        }
        logger.debug { "Health check successful for ${tenant.mnemonic}" }
        return true
    }

    override fun healthCheck(): Map<Tenant, Boolean> =
        tenantService.getMonitoredTenants().associateWith { healthCheck(it) }
}
