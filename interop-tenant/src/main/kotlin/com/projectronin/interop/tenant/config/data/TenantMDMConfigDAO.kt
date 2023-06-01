package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.binding.TenantDOs
import com.projectronin.interop.tenant.config.data.binding.TenantMDMConfigDOs
import com.projectronin.interop.tenant.config.data.model.MirthTenantConfigDO
import com.projectronin.interop.tenant.config.data.model.TenantMDMConfigDO
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
import org.springframework.stereotype.Repository

@Repository
class TenantMDMConfigDAO(@Qualifier("ehr") private val database: Database) {
    fun getByTenantMnemonic(tenantMnemonic: String): TenantMDMConfigDO? {
        return database.from(TenantMDMConfigDOs)
            .leftJoin(TenantDOs, on = TenantMDMConfigDOs.tenantId eq TenantDOs.id)
            .joinReferencesAndSelect()
            .where(TenantDOs.mnemonic eq tenantMnemonic)
            .map { TenantMDMConfigDOs.createEntity(it) }
            .firstOrNull()
    }

    /**
     * Updates a [MirthTenantConfigDO]
     */
    fun updateConfig(updatedDo: TenantMDMConfigDO): TenantMDMConfigDO? {
        database.update(TenantMDMConfigDOs) {
            set(it.mdmDocumentTypeID, updatedDo.mdmDocumentTypeID)
            set(it.providerIdentifierSystem, updatedDo.providerIdentifierSystem)
            set(it.receivingSystem, updatedDo.receivingSystem)

            where {
                it.tenantId eq updatedDo.tenant.id
            }
        }
        return getByTenantId(updatedDo.tenant.id)
    }

    /**
     * Inserts a new [MirthTenantConfigDO]
     */
    fun insertConfig(newDo: TenantMDMConfigDO): TenantMDMConfigDO {
        database.insert(TenantMDMConfigDOs) {
            set(it.tenantId, newDo.tenant.id)
            set(it.mdmDocumentTypeID, newDo.mdmDocumentTypeID)
            set(it.providerIdentifierSystem, newDo.providerIdentifierSystem)
            set(it.receivingSystem, newDo.receivingSystem)
        }
        return getByTenantId(newDo.tenant.id)
            // This should be impossible to hit due to DB constraints
            ?: throw Exception("Inserted MDM tenant config ${newDo.tenant.id} not found")
    }

    private fun getByTenantId(tenantId: Int): TenantMDMConfigDO? {
        val configs = database.from(TenantMDMConfigDOs)
            .select()
            .where(TenantMDMConfigDOs.tenantId eq tenantId)
            .map { TenantMDMConfigDOs.createEntity(it) }
        return configs.singleOrNull()
    }
}
