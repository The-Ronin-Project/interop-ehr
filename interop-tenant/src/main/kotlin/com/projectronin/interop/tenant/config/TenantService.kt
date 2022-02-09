package com.projectronin.interop.tenant.config

import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality for accessing a [Tenant]'s configuration.
 */
interface TenantService {
    /**
     * Retrieves the [Tenant] for the supplied [mnemonic]. If none exists, null will be returned.
     */
    fun getTenantForMnemonic(mnemonic: String): Tenant?

    /**
     * Retrieves the pools associated to the supplied [providerIds] in the [tenantMnemonic]. Any providers that do not have an associated pool will not be included in the response.
     */
    fun getPoolsForProviders(tenantMnemonic: String, providerIds: List<String>): Map<String, String>
}
