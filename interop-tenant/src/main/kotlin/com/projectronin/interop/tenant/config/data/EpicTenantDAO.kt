package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.binding.EpicTenantDOs
import com.projectronin.interop.tenant.config.data.binding.TenantDOs
import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.joinReferencesAndSelect
import org.ktorm.dsl.leftJoin
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.springframework.beans.factory.annotation.Qualifier

/**
 * Provides data access operations for Epic tenant data models.
 */
class EpicTenantDAO(@Qualifier("ehr") private val database: Database) {
    /**
     * inserts a new [epicTenant]
     */
    fun insert(epicTenant: EpicTenantDO): EpicTenantDO? {
        database.insert(EpicTenantDOs) {
            set(it.tenantId, epicTenant.tenantId)
            set(it.release, epicTenant.release)
            set(it.serviceEndpoint, epicTenant.serviceEndpoint)
            set(it.ehrUserId, epicTenant.ehrUserId)
            set(it.messageType, epicTenant.messageType)
            set(it.practitionerProviderSystem, epicTenant.practitionerProviderSystem)
            set(it.practitionerUserSystem, epicTenant.practitionerUserSystem)
            set(it.mrnSystem, epicTenant.mrnSystem)
            set(it.hsi, epicTenant.hsi)
        }
        val epicTenants = database.from(EpicTenantDOs)
            .select()
            .where(EpicTenantDOs.tenantId eq epicTenant.tenantId)
            .map { EpicTenantDOs.createEntity(it) }
        return epicTenants.firstOrNull()
    }

    /**
     * Updates [epicTenant] based on id and returns the number of rows updated.
     */
    fun update(epicTenant: EpicTenantDO): Int {
        return database.update(EpicTenantDOs) {
            set(it.release, epicTenant.release)
            set(it.serviceEndpoint, epicTenant.serviceEndpoint)
            set(it.ehrUserId, epicTenant.ehrUserId)
            set(it.messageType, epicTenant.messageType)
            set(it.practitionerProviderSystem, epicTenant.practitionerProviderSystem)
            set(it.practitionerUserSystem, epicTenant.practitionerUserSystem)
            set(it.mrnSystem, epicTenant.mrnSystem)
            set(it.hsi, epicTenant.hsi)
            where {
                it.tenantId eq epicTenant.tenantId
            }
        }
    }

    /**
     * Returns an [EpicTenantDO] from the table based on the [tenantMnemonic]
     */
    fun getByTenantMnemonic(tenantMnemonic: String): EpicTenantDO? {
        val epicTenants =
            database.from(EpicTenantDOs)
                .leftJoin(TenantDOs, on = EpicTenantDOs.tenantId eq TenantDOs.id)
                .joinReferencesAndSelect()
                .where(TenantDOs.mnemonic eq tenantMnemonic)
                .map { EpicTenantDOs.createEntity(it) }
        return epicTenants.firstOrNull()
    }

    /**
     * Returns all [EpicTenantDO]s in the table
     */
    fun getAll(): List<EpicTenantDO> {
        return database.from(EpicTenantDOs)
            .joinReferencesAndSelect()
            .map { EpicTenantDOs.createEntity(it) }
    }
}
