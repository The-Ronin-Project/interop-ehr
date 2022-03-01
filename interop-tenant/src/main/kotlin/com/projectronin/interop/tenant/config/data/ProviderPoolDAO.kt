package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.binding.ProviderPoolDOs
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.inList
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

/**
 * Provides data access operations for tenant data models.
 */
@Repository
class ProviderPoolDAO(@Qualifier("ehr") private val database: Database) {
    /**
     * Retrieves a Map of pool IDs keyed by provider IDs for the supplied [tenantId] and [providerIds]. If no pool was
     * found for the supplied provider ID, it will not be included in the returned Map.
     */
    fun getPoolsForProviders(tenantId: Int, providerIds: List<String>): Map<String, String> {
        val providerPools = database.from(ProviderPoolDOs).select()
            .where((ProviderPoolDOs.tenantId eq tenantId) and (ProviderPoolDOs.providerId inList providerIds))
            .map { ProviderPoolDOs.createEntity(it) }
        return providerPools.associate { it.providerId to it.poolId }
    }
}
