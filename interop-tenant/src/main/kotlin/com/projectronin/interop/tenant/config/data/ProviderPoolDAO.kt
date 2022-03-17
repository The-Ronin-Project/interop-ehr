package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.binding.ProviderPoolDOs
import com.projectronin.interop.tenant.config.data.model.ProviderPoolDO
import mu.KotlinLogging
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.inList
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

/**
 * Provides data access operations for tenant data models.
 */
@Repository
class ProviderPoolDAO(@Qualifier("ehr") private val database: Database) {
    private val logger = KotlinLogging.logger { }
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

    fun insert(providerPool: ProviderPoolDO): ProviderPoolDO? {
        val providerPoolKey = try {
            database.insertAndGenerateKey(ProviderPoolDOs) {
                set(it.tenantId, providerPool.tenantId.id)
                set(it.providerId, providerPool.providerId)
                set(it.poolId, providerPool.poolId)
            }
        } catch (e: Exception) {
            logger.error(e) { "insert failed: $e" }
            return null
        }
        providerPool.id = providerPoolKey as Long
        return providerPool
    }

    fun update(providerPool: ProviderPoolDO): Int? {
        return try {
            database.update(ProviderPoolDOs) {
                set(it.tenantId, providerPool.tenantId.id)
                set(it.providerId, providerPool.providerId)
                set(it.poolId, providerPool.poolId)
                where {
                    it.id eq providerPool.id
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "update failed: $e" }
            null
        }
    }

    fun delete(providerPoolId: Long): Int? = try {
        database.delete(ProviderPoolDOs) { it.id eq providerPoolId }
    } catch (e: Exception) {
        logger.error(e) { "delete failed: $e" }
        null
    }
}
