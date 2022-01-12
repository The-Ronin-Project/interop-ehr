package com.projectronin.interop.ehr.auth

import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Brokers [Authentication] allowing re-use of existing credentials as long as they have not expired.
 */
@Component
class AuthenticationBroker(authenticationServices: List<AuthenticationService>) {
    // Ideally, we could reuse the VendorFactory here, but this creates circular dependencies:
    // Factory requires Service, which requires Client, which requires Broker, which requires Factory
    // So we're following the same model used by the VendorFactory and EHRFactory within the AuthenticationService and here.
    private val authenticationServiceByVendor = authenticationServices.associateBy { it.vendorType }

    private val expirationBuffer: Long = 60 // seconds
    private val authenticationByTenant = mutableMapOf<String, Authentication>()

    /**
     * Retrieves the current [Authentication] for the supplied [Tenant].
     */
    fun getAuthentication(tenant: Tenant): Authentication? {
        val authentication = authenticationByTenant[tenant.mnemonic].takeIf {
            it?.expiresAt?.isAfter(
                Instant.now().plusSeconds(expirationBuffer)
            ) ?: false
        }

        authentication?.let { return authentication }

        val authenticationService = authenticationServiceByVendor[tenant.vendor.type]
            ?: throw IllegalStateException("No AuthenticationService registered for ${tenant.vendor.type}")

        val newAuthentication = authenticationService.getAuthentication(tenant)
        newAuthentication?.let { authenticationByTenant.put(tenant.mnemonic, newAuthentication) }
        return newAuthentication
    }
}
