package com.projectronin.interop.ehr.auth

import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.auth.BrokeredAuthenticator
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Brokers [Authentication] allowing re-use of existing credentials as long as they have not expired.
 */
@Component
class EHRAuthenticationBroker(authenticationServices: List<AuthenticationService>) {
    // Ideally, we could reuse the VendorFactory here, but this creates circular dependencies:
    // Factory requires Service, which requires Client, which requires Broker, which requires Factory
    // So we're following the same model used by the VendorFactory and EHRFactory within the AuthenticationService and here.
    private val authenticatorByVendorType =
        authenticationServices.associateBy { it.vendorType }
    private val brokerByTenant = mutableMapOf<String, BrokeredAuthenticator>()

    /**
     * Retrieves the current [BrokeredAuthenticator] for the supplied [Tenant].
     */
    fun getAuthenticator(tenant: Tenant): BrokeredAuthenticator =
        brokerByTenant.computeIfAbsent(tenant.mnemonic) {
            val vendorType = tenant.vendor.type
            val authenticationService =
                authenticatorByVendorType[vendorType]
                    ?: throw IllegalStateException("No AuthenticationService registered for $vendorType")
            TenantAuthenticationBroker(authenticationService, tenant)
        }
}
