package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.binding.TenantCodesDOs
import com.projectronin.interop.tenant.config.data.binding.TenantDOs
import com.projectronin.interop.tenant.config.data.model.TenantCodesDO
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
class TenantCodesDAO(@Qualifier("ehr") private val database: Database) {

    /**
     * Returns all [TenantCodesDO]s in the table
     */
    fun getAll(): List<TenantCodesDO> {
        return database.from(TenantCodesDOs)
            .joinReferencesAndSelect()
            .map { TenantCodesDOs.createEntity(it) }
    }

    /**
     * Returns a [TenantCodesDO] by the given tenantMnemonic
     */
    fun getByTenantMnemonic(tenantMnemonic: String): TenantCodesDO? {
        return database.from(TenantCodesDOs)
            .leftJoin(TenantDOs, on = TenantCodesDOs.tenantId eq TenantDOs.id)
            .joinReferencesAndSelect()
            .where(TenantDOs.mnemonic eq tenantMnemonic)
            .map { TenantCodesDOs.createEntity(it) }
            .firstOrNull()
    }

    /**
     * Updates a [TenantCodesDO]
     */
    fun updateCodes(tenantCodesDO: TenantCodesDO): TenantCodesDO? {
        database.update(TenantCodesDOs) {
            set(it.bsaCode, tenantCodesDO.bsaCode)
            set(it.bmiCode, tenantCodesDO.bmiCode)
            set(it.stageCodes, tenantCodesDO.stageCodes)
            where {
                it.tenantId eq tenantCodesDO.tenantId
            }
        }
        return getByTenantId(tenantCodesDO.tenantId)
    }

    /**
     * Inserts a new [TenantCodesDO]
     */
    fun insertCodes(tenantCodesDO: TenantCodesDO): TenantCodesDO {
        database.insert(TenantCodesDOs) {
            set(it.tenantId, tenantCodesDO.tenantId)
            set(it.bsaCode, tenantCodesDO.bsaCode)
            set(it.bmiCode, tenantCodesDO.bmiCode)
            set(it.stageCodes, tenantCodesDO.stageCodes)
        }
        return getByTenantId(tenantCodesDO.tenantId)
            // This should be impossible to hit due to DB constraints
            ?: throw Exception("Inserted tenant codes ${tenantCodesDO.tenantId} not found")
    }

    /**
     * Returns a [TenantCodesDO] by the given [tenantId].  If a [TenantCodesDO] doesn't exist for
     * [tenantId], or multiple somehow do, it returns null.
     */
    private fun getByTenantId(tenantId: Int): TenantCodesDO? {
        val tenantCodes = database.from(TenantCodesDOs)
            .select()
            .where(TenantCodesDOs.tenantId eq tenantId)
            .map { TenantCodesDOs.createEntity(it) }
        return tenantCodes.singleOrNull()
    }
}
