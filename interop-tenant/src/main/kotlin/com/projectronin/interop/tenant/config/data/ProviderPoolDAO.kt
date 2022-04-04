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
     * Retrieves a list of [ProviderPoolDO]s for the supplied [tenantId] and [providerIds].
     */
    fun getPoolsForProviders(tenantId: Int, providerIds: List<String>): List<ProviderPoolDO> {
        return database.from(ProviderPoolDOs).select()
            .where((ProviderPoolDOs.tenantId eq tenantId) and (ProviderPoolDOs.providerId inList providerIds))
            .map { ProviderPoolDOs.createEntity(it) }
    }

    /**
     * Retrieves a list of [ProviderPoolDO]s for the supplied [tenantId].
     */
    fun getAll(tenantId: Int): List<ProviderPoolDO> {
        return database.from(ProviderPoolDOs).select().where(ProviderPoolDOs.tenantId eq tenantId)
            .map { ProviderPoolDOs.createEntity(it) }
    }

    fun insert(providerPool: ProviderPoolDO): ProviderPoolDO {
        val providerPoolKey = try {
            database.insertAndGenerateKey(ProviderPoolDOs) {
                set(it.tenantId, providerPool.tenant.id)
                set(it.providerId, providerPool.providerId)
                set(it.poolId, providerPool.poolId)
            }
        } catch (e: Exception) {
            logger.warn(e) { "insert failed: $e" }
            throw e
        }
        providerPool.id = providerPoolKey as Long
        return providerPool
    }

    fun update(providerPool: ProviderPoolDO): Int {
        return try {
            database.update(ProviderPoolDOs) {
                set(it.tenantId, providerPool.tenant.id)
                set(it.providerId, providerPool.providerId)
                set(it.poolId, providerPool.poolId)
                where {
                    it.id eq providerPool.id
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "update failed: $e" }
            throw e
        }
    }

    fun delete(providerPoolId: Long): Int = try {
        database.delete(ProviderPoolDOs) { it.id eq providerPoolId }
    } catch (e: Exception) {
        logger.error(e) { "delete failed: $e" }
        throw e
    }
}
