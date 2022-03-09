package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.binding.EpicTenantDOs
import com.projectronin.interop.tenant.config.data.binding.TenantDOs
import com.projectronin.interop.tenant.config.data.model.EHRTenantDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import mu.KotlinLogging
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.dsl.joinReferencesAndSelect
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

/**
 * Provides data access operations for tenant data models.
 */
@Repository
class TenantDAO(@Qualifier("ehr") private val database: Database) {
    private val logger = KotlinLogging.logger { }

    /**
     * Retrieves a [TenantDO] for the supplied [mnemonic]. If the mnemonic is unknown, null will be returned.
     */
    fun getTenantForMnemonic(mnemonic: String): TenantDO? {
        val tenants =
            database.from(TenantDOs).joinReferencesAndSelect().where(TenantDOs.mnemonic eq mnemonic)
                .map { TenantDOs.createEntity(it) }
        return tenants.getOrNull(0)
    }

    /**
     * Retrieves an [EHRTenantDO] for the supplied [tenantId] and [vendorType]. If no vendor specific configuration is found for the tenant, null will be returned.
     */
    fun <T : EHRTenantDO> getEHRTenant(tenantId: Int, vendorType: VendorType): T? {
        // When multiple vendors exist, we should use a when clause across the vendors.
        val ehrTenants = database.from(EpicTenantDOs).select().where(EpicTenantDOs.tenantId eq tenantId)
            .map { EpicTenantDOs.createEntity(it) }

        return ehrTenants.getOrNull(0) as T?
    }

    /**
     * Inserts new [tenant].  Creates a new id for the [tenant] regardless if one is already set or not, and returns
     * the same tenant with the new id inserted.
     */
    fun insertTenant(tenant: TenantDO): TenantDO {
        val tenantKey = try {
            database.insertAndGenerateKey(TenantDOs) {
                set(it.mnemonic, tenant.mnemonic)
                set(it.ehr, tenant.ehr.id)
                set(it.availableBatchStart, tenant.availableBatchStart)
                set(it.availableBatchEnd, tenant.availableBatchEnd)
            }
        } catch (e: Exception) {
            logger.error(e) { "Tenant insert failed: $e" }
            throw e
        }
        tenant.id = tenantKey.toString().toInt()
        return tenant
    }

    /**
     * Updates [tenant] based on id and returns the number of rows updated.
     */
    fun updateTenant(tenant: TenantDO): Int {
        return try {
            database.update(TenantDOs) {
                set(it.mnemonic, tenant.mnemonic)
                set(it.ehr, tenant.ehr.id)
                set(it.availableBatchStart, tenant.availableBatchStart)
                set(it.availableBatchEnd, tenant.availableBatchEnd)
                where {
                    it.id eq tenant.id
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Tenant update failed: $e" }
            throw e
        }
    }
}
