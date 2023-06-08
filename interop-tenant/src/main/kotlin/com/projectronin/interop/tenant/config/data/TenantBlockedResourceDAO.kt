package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.binding.TenantBlockedResourceDOs
import com.projectronin.interop.tenant.config.data.binding.TenantDOs
import com.projectronin.interop.tenant.config.data.model.TenantBlockedResourceDO
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.joinReferencesAndSelect
import org.ktorm.dsl.leftJoin
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

@Repository
class TenantBlockedResourceDAO(@Qualifier("ehr") private val database: Database) {

    /**
     * Returns all [TenantBlockedResourceDO]s in the table
     */
    fun getAll(): List<TenantBlockedResourceDO> {
        return database
            .from(TenantBlockedResourceDOs)
            .joinReferencesAndSelect()
            .map { TenantBlockedResourceDOs.createEntity(it) }
    }

    /**
     * Returns all [TenantBlockedResourceDO]s by the given tenantMnemonic
     */
    fun getByTenantMnemonic(tenantMnemonic: String): List<TenantBlockedResourceDO> {
        return database.from(TenantBlockedResourceDOs)
            .leftJoin(TenantDOs, on = TenantBlockedResourceDOs.tenantId eq TenantDOs.id)
            .joinReferencesAndSelect()
            .where(TenantDOs.mnemonic eq tenantMnemonic)
            .map { TenantBlockedResourceDOs.createEntity(it) }
    }

    /**
     * Inserts a new [TenantBlockedResourceDO]
     */
    fun insertBlockedResource(tenantBlockedResourceDO: TenantBlockedResourceDO): TenantBlockedResourceDO {
        database.insert(TenantBlockedResourceDOs) {
            set(it.tenantId, tenantBlockedResourceDO.tenantId)
            set(it.resource, tenantBlockedResourceDO.resource)
        }

        tenantBlockedResourceDO.apply {
            return getByTenantIdAndResource(this.tenantId, this.resource)
                // This should be impossible to hit due to DB constraints
                ?: throw Exception("Inserted tenant blocked resource [tenant: ${this.tenantId}, resource: ${this.resource}] not found")
        }
    }

    /**
     * Deletes a [TenantBlockedResourceDO]
     */
    fun deleteBlockedResource(tenantBlockedResourceDO: TenantBlockedResourceDO): Int {
        return database.delete(TenantBlockedResourceDOs) {
            (it.tenantId eq tenantBlockedResourceDO.tenantId) and (it.resource eq tenantBlockedResourceDO.resource)
        }
    }

    /**
     * Returns a specific [TenantBlockedResourceDO] by [tenantId] and [resource] name
     */
    private fun getByTenantIdAndResource(tenantId: Int, resource: String): TenantBlockedResourceDO? {
        return database.from(TenantBlockedResourceDOs)
            .select()
            .where((TenantBlockedResourceDOs.tenantId eq tenantId) and (TenantBlockedResourceDOs.resource eq resource))
            .map { TenantBlockedResourceDOs.createEntity(it) }
            .firstOrNull()
    }
}
