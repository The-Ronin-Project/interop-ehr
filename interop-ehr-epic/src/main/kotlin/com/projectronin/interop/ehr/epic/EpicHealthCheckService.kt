package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.HealthCheckService
import com.projectronin.interop.ehr.epic.auth.EpicAuthenticationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.tenant.config.TenantService
import com.projectronin.interop.tenant.config.model.Tenant
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class EpicHealthCheckService(
    private val authorizationService: EpicAuthenticationService,
    private val epicClient: EpicClient,
    private val tenantService: TenantService
) : HealthCheckService {
    private val logger = KotlinLogging.logger { }

    override fun healthCheck(tenant: Tenant): Boolean {
        try {
            authorizationService.getAuthentication(tenant, true)
        } catch (e: Exception) {
            logger.debug(e) { "Failed authorization health check for ${tenant.mnemonic}" }
            return false
        }
        try {
            runBlocking {
                epicClient.options(tenant, "/api/FHIR/R4/")
            }
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
