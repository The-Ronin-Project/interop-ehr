package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.binding.MirthTenantConfigDOs
import com.projectronin.interop.tenant.config.data.binding.TenantDOs
import com.projectronin.interop.tenant.config.data.model.MirthTenantConfigDO
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
class MirthTenantConfigDAO(@Qualifier("ehr") private val database: Database) {

    /**
     * Returns all [MirthTenantConfigDO]s in the table
     */
    fun getAll(): List<MirthTenantConfigDO> {
        return database.from(MirthTenantConfigDOs)
            .joinReferencesAndSelect()
            .map { MirthTenantConfigDOs.createEntity(it) }
    }

    /**
     * Returns a [MirthTenantConfigDO] by the given tenantMnemonic
     */
    fun getByTenantMnemonic(tenantMnemonic: String): MirthTenantConfigDO? {
        return database.from(MirthTenantConfigDOs)
            .leftJoin(TenantDOs, on = MirthTenantConfigDOs.tenantId eq TenantDOs.id)
            .joinReferencesAndSelect()
            .where(TenantDOs.mnemonic eq tenantMnemonic)
            .map { MirthTenantConfigDOs.createEntity(it) }
            .firstOrNull()
    }

    /**
     * Updates a [MirthTenantConfigDO]
     */
    fun updateConfig(mirthTenantConfigDO: MirthTenantConfigDO): Int {
        return database.update(MirthTenantConfigDOs) {
            set(it.locationIds, mirthTenantConfigDO.locationIds)
            where {
                it.tenantId eq mirthTenantConfigDO.tenant.id
            }
        }
    }

    /**
     * Inserts a new [MirthTenantConfigDO]
     */
    fun insertConfig(mirthTenantConfigDO: MirthTenantConfigDO): MirthTenantConfigDO {
        database.insert(MirthTenantConfigDOs) {
            set(it.tenantId, mirthTenantConfigDO.tenant.id)
            set(it.locationIds, mirthTenantConfigDO.locationIds)
        }

        val mirthTenantConfigs = database.from(MirthTenantConfigDOs)
            .select()
            .where(MirthTenantConfigDOs.tenantId eq mirthTenantConfigDO.tenant.id)
            .map { MirthTenantConfigDOs.createEntity(it) }
        return mirthTenantConfigs.single()
    }
}
