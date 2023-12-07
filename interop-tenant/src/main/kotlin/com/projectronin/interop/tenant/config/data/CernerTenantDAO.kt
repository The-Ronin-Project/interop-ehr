package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.binding.CernerTenantDOs
import com.projectronin.interop.tenant.config.data.binding.TenantDOs
import com.projectronin.interop.tenant.config.data.model.CernerTenantDO
import com.projectronin.interop.tenant.config.data.model.EHRTenantDO
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

/**
 * Provides data access operations for Cerner tenant data models.
 */
@Repository
class CernerTenantDAO(
    @Qualifier("ehr") private val database: Database,
) : EHRTenantDAO {
    override fun insert(ehrTenantDO: EHRTenantDO): CernerTenantDO {
        val cernerTenant = ehrTenantDO as CernerTenantDO
        database.insert(CernerTenantDOs) {
            set(it.tenantId, cernerTenant.tenantId)
            set(it.serviceEndpoint, cernerTenant.serviceEndpoint)
            set(it.patientMRNSystem, cernerTenant.patientMRNSystem)
            set(it.authEndpoint, cernerTenant.authEndpoint)
            set(it.messagePractitioner, cernerTenant.messagePractitioner)
            set(it.messageTopic, cernerTenant.messageTopic)
            set(it.messageCategory, cernerTenant.messageCategory)
            set(it.messagePriority, cernerTenant.messagePriority)
        }
        val cernerTenants =
            database.from(CernerTenantDOs)
                .select()
                .where(CernerTenantDOs.tenantId eq cernerTenant.tenantId)
                .map { CernerTenantDOs.createEntity(it) }
        return cernerTenants.single()
    }

    /**
     * Updates [CernerTenantDO] based on id and returns the number of rows updated.
     */
    override fun update(ehrTenantDO: EHRTenantDO): Int {
        val cernerTenant = ehrTenantDO as CernerTenantDO
        return database.update(CernerTenantDOs) {
            set(it.serviceEndpoint, cernerTenant.serviceEndpoint)
            set(it.patientMRNSystem, cernerTenant.patientMRNSystem)
            set(it.authEndpoint, cernerTenant.authEndpoint)
            set(it.messagePractitioner, cernerTenant.messagePractitioner)
            set(it.messageTopic, cernerTenant.messageTopic)
            set(it.messageCategory, cernerTenant.messageCategory)
            set(it.messagePriority, cernerTenant.messagePriority)
            where {
                it.tenantId eq cernerTenant.tenantId
            }
        }
    }

    /**
     * Returns an [CernerTenantDO] from the table based on the [tenantMnemonic]
     */
    override fun getByTenantMnemonic(tenantMnemonic: String): CernerTenantDO? {
        val cernerTenants =
            database.from(CernerTenantDOs)
                .leftJoin(TenantDOs, on = CernerTenantDOs.tenantId eq TenantDOs.id)
                .joinReferencesAndSelect()
                .where(TenantDOs.mnemonic eq tenantMnemonic)
                .map { CernerTenantDOs.createEntity(it) }
        return cernerTenants.firstOrNull()
    }

    /**
     * Returns all [CernerTenantDO]s in the table
     */
    override fun getAll(): List<CernerTenantDO> {
        return database.from(CernerTenantDOs)
            .joinReferencesAndSelect()
            .map { CernerTenantDOs.createEntity(it) }
    }
}
