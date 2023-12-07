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
import org.ktorm.dsl.joinReferencesAndSelect
import org.ktorm.dsl.map
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

/**
 * Provides data access operations for tenant data models.
 */
@Repository
class ProviderPoolDAO(
    @Qualifier("ehr") private val database: Database,
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Retrieves a list of [ProviderPoolDO]s for the supplied [tenantId] and [providerIds].
     */
    fun getPoolsForProviders(
        tenantId: Int,
        providerIds: List<String>,
    ): List<ProviderPoolDO> {
        return database.from(ProviderPoolDOs).joinReferencesAndSelect()
            .where((ProviderPoolDOs.tenantId eq tenantId) and (ProviderPoolDOs.providerId inList providerIds))
            .map { ProviderPoolDOs.createEntity(it) }
    }

    /**
     * Retrieves a list of [ProviderPoolDO]s for the supplied [tenantId].
     */
    fun getAll(tenantId: Int): List<ProviderPoolDO> {
        return database.from(ProviderPoolDOs).joinReferencesAndSelect().where(ProviderPoolDOs.tenantId eq tenantId)
            .map { ProviderPoolDOs.createEntity(it) }
    }

    /**
     * Retrieves a single [ProviderPoolDO] based on its [providerPoolId].  Returns null if not found.
     */
    fun getPoolById(providerPoolId: Int): ProviderPoolDO? {
        return database.from(ProviderPoolDOs)
            .joinReferencesAndSelect()
            .where(ProviderPoolDOs.id eq providerPoolId)
            .map { ProviderPoolDOs.createEntity(it) }.singleOrNull()
    }

    fun insert(providerPool: ProviderPoolDO): ProviderPoolDO {
        val newProviderPoolId =
            database.insertAndGenerateKey(ProviderPoolDOs) {
                set(it.tenantId, providerPool.tenant.id)
                set(it.providerId, providerPool.providerId)
                set(it.poolId, providerPool.poolId)
            } as Int
        return getPoolById(newProviderPoolId)
            // This should be impossible to hit due to DB constraints
            ?: throw Exception("Inserted ProviderPool ${providerPool.id} not found")
    }

    fun update(providerPool: ProviderPoolDO): ProviderPoolDO? {
        database.update(ProviderPoolDOs) {
            set(it.tenantId, providerPool.tenant.id)
            set(it.providerId, providerPool.providerId)
            set(it.poolId, providerPool.poolId)
            where {
                it.id eq providerPool.id
            }
        }
        return getPoolById(providerPool.id)
    }

    fun delete(providerPoolId: Int): Int = database.delete(ProviderPoolDOs) { it.id eq providerPoolId }
}
