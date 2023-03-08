package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.binding.EpicTenantDOs
import com.projectronin.interop.tenant.config.data.binding.TenantDOs
import com.projectronin.interop.tenant.config.data.model.EHRTenantDO
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
import org.springframework.stereotype.Repository

/**
 * Provides data access operations for Epic tenant data models.
 */
@Repository
class EpicTenantDAO(@Qualifier("ehr") private val database: Database) : EHRTenantDAO {
    /**
     * inserts a new [EpicTenantDO]
     */
    override fun insert(ehrTenantDO: EHRTenantDO): EpicTenantDO {
        val epicTenant = ehrTenantDO as EpicTenantDO
        database.insert(EpicTenantDOs) {
            set(it.tenantId, epicTenant.tenantId)
            set(it.release, epicTenant.release)
            set(it.serviceEndpoint, epicTenant.serviceEndpoint)
            set(it.authEndpoint, epicTenant.authEndpoint)
            set(it.ehrUserId, epicTenant.ehrUserId)
            set(it.messageType, epicTenant.messageType)
            set(it.practitionerProviderSystem, epicTenant.practitionerProviderSystem)
            set(it.practitionerUserSystem, epicTenant.practitionerUserSystem)
            set(it.patientMRNSystem, epicTenant.patientMRNSystem)
            set(it.patientInternalSystem, epicTenant.patientInternalSystem)
            set(it.encounterCSNSystem, epicTenant.encounterCSNSystem)
            set(it.patientMRNTypeText, epicTenant.patientMRNTypeText)
            set(it.hsi, epicTenant.hsi)
            set(it.departmentInternalSystem, epicTenant.departmentInternalSystem)
            set(it.patientOnboardedFlagId, epicTenant.patientOnboardedFlagId)
        }
        val epicTenants = database.from(EpicTenantDOs)
            .select()
            .where(EpicTenantDOs.tenantId eq epicTenant.tenantId)
            .map { EpicTenantDOs.createEntity(it) }
        return epicTenants.single()
    }

    /**
     * Updates [EpicTenantDO] based on id and returns the number of rows updated.
     */
    override fun update(ehrTenantDO: EHRTenantDO): Int {
        val epicTenant = ehrTenantDO as EpicTenantDO
        return database.update(EpicTenantDOs) {
            set(it.release, epicTenant.release)
            set(it.serviceEndpoint, epicTenant.serviceEndpoint)
            set(it.authEndpoint, epicTenant.authEndpoint)
            set(it.ehrUserId, epicTenant.ehrUserId)
            set(it.messageType, epicTenant.messageType)
            set(it.practitionerProviderSystem, epicTenant.practitionerProviderSystem)
            set(it.practitionerUserSystem, epicTenant.practitionerUserSystem)
            set(it.patientMRNSystem, epicTenant.patientMRNSystem)
            set(it.patientInternalSystem, epicTenant.patientInternalSystem)
            set(it.encounterCSNSystem, epicTenant.encounterCSNSystem)
            set(it.patientMRNTypeText, epicTenant.patientMRNTypeText)
            set(it.hsi, epicTenant.hsi)
            set(it.departmentInternalSystem, epicTenant.departmentInternalSystem)
            set(it.patientOnboardedFlagId, epicTenant.patientOnboardedFlagId)
            where {
                it.tenantId eq epicTenant.tenantId
            }
        }
    }

    /**
     * Returns an [EpicTenantDO] from the table based on the [tenantMnemonic]
     */
    override fun getByTenantMnemonic(tenantMnemonic: String): EpicTenantDO? {
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
    override fun getAll(): List<EpicTenantDO> {
        return database.from(EpicTenantDOs)
            .joinReferencesAndSelect()
            .map { EpicTenantDOs.createEntity(it) }
    }
}
