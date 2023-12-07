package com.projectronin.interop.tenant.config

import com.projectronin.interop.tenant.config.data.ProviderPoolDAO
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Service

/**
 * Service responsible for loading and managing provider pools
 */
@Service
class ProviderPoolService(private val providerPoolDAO: ProviderPoolDAO) {
    /**
     * Retrieves the pools associated to the supplied [providerIds] in the [tenant]. The provider IDs are expected to be
     * the internal tenant representation of a provider and not something like a FHIR ID.  The returned Map will be
     * keyed by the provider IDs and valued by the pool IDs. Any providers that do not have an associated pool will not
     * be included in the response.
     */
    fun getPoolsForProviders(
        tenant: Tenant,
        providerIds: List<String>,
    ): Map<String, String> {
        return providerPoolDAO.getPoolsForProviders(tenant.internalId, providerIds)
            .associate { it.providerId to it.poolId }
    }
}
