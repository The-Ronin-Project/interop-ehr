package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.HealthCheckService
import com.projectronin.interop.ehr.cerner.auth.CernerAuthenticationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.tenant.config.TenantService
import com.projectronin.interop.tenant.config.model.Tenant
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class CernerHealthCheckService(
    private val authorizationService: CernerAuthenticationService,
    private val cernerClient: CernerClient,
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
            val parameters = mapOf(
                "given" to "Health",
                "family:exact" to "Check",
                "birthdate" to DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())
            )
            runBlocking {
                cernerClient.options(tenant, "/Patient", parameters)
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
