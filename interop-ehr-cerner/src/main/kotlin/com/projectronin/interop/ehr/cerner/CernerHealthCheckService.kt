package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.HealthCheckService
import com.projectronin.interop.ehr.cerner.auth.CernerAuthenticationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.tenant.config.TenantService
import com.projectronin.interop.tenant.config.model.Tenant
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class CernerHealthCheckService(
    private val authorizationService: CernerAuthenticationService,
    private val cernerClient: CernerClient,
    private val tenantService: TenantService,
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
                cernerClient.options(tenant, "/")
            }
        } catch (e: Exception) {
            logger.debug(e) { "Failed Patient search health check for ${tenant.mnemonic}" }
            return false
        }
        logger.debug { "Health check successful for ${tenant.mnemonic}" }
        return true
    }

    override fun healthCheck(): Map<Tenant, Boolean> = tenantService.getMonitoredTenants().associateWith { healthCheck(it) }
}
