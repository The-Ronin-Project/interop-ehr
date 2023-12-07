package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.common.hl7.MessageType
import com.projectronin.interop.tenant.config.data.binding.TenantDOs
import com.projectronin.interop.tenant.config.data.binding.TenantServerDOs
import com.projectronin.interop.tenant.config.data.model.TenantServerDO
import mu.KotlinLogging
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.dsl.joinReferencesAndSelect
import org.ktorm.dsl.leftJoin
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.dsl.whereWithConditions
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

/**
 * Provides data access operations for tenant server data models.
 */
@Repository
class TenantServerDAO(
    @Qualifier("ehr") private val database: Database,
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Retrieves all [TenantServerDO]s for a given tenant
     */
    fun getTenantServers(
        tenantMnemonic: String,
        type: MessageType? = null,
    ): List<TenantServerDO> {
        return database.from(TenantServerDOs)
            .leftJoin(TenantDOs, on = TenantServerDOs.tenantId eq TenantDOs.id)
            .joinReferencesAndSelect()
            .whereWithConditions {
                it += TenantDOs.mnemonic eq tenantMnemonic
                if (type != null) {
                    it += TenantServerDOs.messageType eq type
                }
            }
            .map { TenantServerDOs.createEntity(it) }
    }

    /**
     * Retrieves a [TenantServerDO] by id
     */
    fun getTenantServer(id: Int): TenantServerDO? {
        return database.from(TenantServerDOs)
            .select()
            .where(TenantServerDOs.id eq id)
            .map { TenantServerDOs.createEntity(it) }
            .firstOrNull()
    }

    /**
     * Create a new [TenantServerDO]
     */
    fun insertTenantServer(tenantServer: TenantServerDO): TenantServerDO {
        val tenantKey =
            try {
                database.insertAndGenerateKey(TenantServerDOs) {
                    set(it.tenantId, tenantServer.tenant.id)
                    set(it.messageType, tenantServer.messageType)
                    set(it.address, tenantServer.address)
                    set(it.port, tenantServer.port)
                    set(it.serverType, tenantServer.serverType)
                }
            } catch (e: Exception) {
                logger.error(e) { "TenantServer insert failed: $e" }
                throw e
            }
        tenantServer.id = tenantKey.toString().toInt()
        return tenantServer
    }

    /**
     * Update an existing [TenantServerDO]
     */
    fun updateTenantServer(tenantServer: TenantServerDO): TenantServerDO? {
        database.update(TenantServerDOs) {
            set(it.messageType, tenantServer.messageType)
            set(it.address, tenantServer.address)
            set(it.port, tenantServer.port)
            set(it.serverType, tenantServer.serverType)
            where {
                (it.tenantId eq tenantServer.tenant.id) and (it.messageType eq tenantServer.messageType)
            }
        }
        return getTenantServer(tenantServer.id)
    }
}
