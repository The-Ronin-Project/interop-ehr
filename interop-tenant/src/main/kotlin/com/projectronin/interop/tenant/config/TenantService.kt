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
     * Retrieves the pools associated to the supplied [providerIds] in the [tenant]. The provider IDs are expected to be the internal tenant representation of a provider and not something like a FHIR ID.
     * The returned Map will be keyed by the provider IDs and valued by the pool IDs. Any providers that do not have an associated pool will not be included in the response.
     */
    fun getPoolsForProviders(tenant: Tenant, providerIds: List<String>): Map<String, String>
}
