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
    fun updateConfig(mirthTenantConfigDO: MirthTenantConfigDO): MirthTenantConfigDO? {
        database.update(MirthTenantConfigDOs) {
            set(it.locationIds, mirthTenantConfigDO.locationIds)
            where {
                it.tenantId eq mirthTenantConfigDO.tenant.id
            }
        }
        return getByTenantId(mirthTenantConfigDO.tenant.id)
    }

    /**
     * Inserts a new [MirthTenantConfigDO]
     */
    fun insertConfig(mirthTenantConfigDO: MirthTenantConfigDO): MirthTenantConfigDO {
        database.insert(MirthTenantConfigDOs) {
            set(it.tenantId, mirthTenantConfigDO.tenant.id)
            set(it.locationIds, mirthTenantConfigDO.locationIds)
        }
        return getByTenantId(mirthTenantConfigDO.tenant.id)
            // This should be impossible to hit due to DB constraints
            ?: throw Exception("Inserted tenant config ${mirthTenantConfigDO.tenant.id} not found")
    }

    /**
     * Returns a [MirthTenantConfigDO] by the given [tenantId].  If a [MirthTenantConfigDO] doesn't exist for
     * [tenantId], or multiple somehow do, it returns null.
     */
    private fun getByTenantId(tenantId: Int): MirthTenantConfigDO? {
        val mirthTenantConfigs = database.from(MirthTenantConfigDOs)
            .select()
            .where(MirthTenantConfigDOs.tenantId eq tenantId)
            .map { MirthTenantConfigDOs.createEntity(it) }
        return mirthTenantConfigs.singleOrNull()
    }
}
