package com.projectronin.interop.tenant.config

import com.projectronin.interop.tenant.config.data.ProviderPoolDAO
import com.projectronin.interop.tenant.config.data.TenantDAO
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import com.projectronin.interop.tenant.config.model.AuthenticationConfig
import com.projectronin.interop.tenant.config.model.BatchConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import com.projectronin.interop.tenant.config.model.vendor.Vendor
import mu.KotlinLogging

/**
 * Service responsible for [Tenant]s loaded from a database.
 */
class TenantServiceDB(private val tenantDAO: TenantDAO, private val providerPoolDAO: ProviderPoolDAO) : TenantService {
    private val logger = KotlinLogging.logger { }

    override fun getTenantForMnemonic(mnemonic: String): Tenant? {
        logger.info { "Retrieving tenant for mnemonic : $mnemonic" }
        val tenantDO = tenantDAO.getTenantForMnemonic(mnemonic) ?: return null
        val ehrDO = tenantDO.ehr

        // This really needs to be doing a vendor check and determining what object to retrieve and what object to create for the response,
        // but we only support Epic today, and Jacoco has some coverage issues when only one option exists in a when clause.
        val ehrTenantDO = tenantDAO.getEHRTenant<EpicTenantDO>(tenantDO.id, ehrDO.vendorType) ?: return null

        val vendor = createEpicVendor(ehrDO, ehrTenantDO)
        return createTenant(tenantDO, vendor)
    }

    override fun getPoolsForProviders(tenant: Tenant, providerIds: List<String>): Map<String, String> {
        return providerPoolDAO.getPoolsForProviders(tenant.internalId, providerIds)
    }

    private fun createTenant(tenantDO: TenantDO, vendor: Vendor): Tenant {
        val batchConfig = createBatchConfig(tenantDO)
        return Tenant(
            internalId = tenantDO.id,
            mnemonic = tenantDO.mnemonic,
            batchConfig = batchConfig,
            vendor = vendor
        )
    }

    private fun createBatchConfig(tenantDO: TenantDO) =
        if (tenantDO.availableBatchStart != null && tenantDO.availableBatchEnd != null) {
            BatchConfig(
                availableStart = tenantDO.availableBatchStart!!,
                availableEnd = tenantDO.availableBatchEnd!!
            )
        } else {
            null
        }

    private fun createEpicVendor(ehrDO: EhrDO, epicTenant: EpicTenantDO): Epic {
        val authenticationConfig = AuthenticationConfig(
            publicKey = ehrDO.publicKey,
            privateKey = ehrDO.privateKey
        )
        return Epic(
            clientId = ehrDO.clientId,
            authenticationConfig = authenticationConfig,
            serviceEndpoint = epicTenant.serviceEndpoint,
            release = epicTenant.release,
            ehrUserId = epicTenant.ehrUserId,
            messageType = epicTenant.messageType,
            practitionerProviderSystem = epicTenant.practitionerProviderSystem,
            practitionerUserSystem = epicTenant.practitionerUserSystem
        )
    }
}
