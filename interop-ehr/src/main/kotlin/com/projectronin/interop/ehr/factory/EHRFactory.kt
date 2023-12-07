package com.projectronin.interop.ehr.factory

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Service

/**
 * Provides access to appropriate [vendor factories][VendorFactory] capable of accessing vendor-specific services.
 */
@Service
class EHRFactory(vendorFactories: List<VendorFactory>) {
    private val vendorFactoriesByType: Map<VendorType, VendorFactory> = vendorFactories.associateBy { it.vendorType }

    /**
     * Retrieves the [VendorFactory] appropriate for the supplied Tenant. If no [VendorFactory] is registered for the Tenant's [VendorType], an [IllegalStateException] will be thrown.
     */
    fun getVendorFactory(tenant: Tenant) =
        vendorFactoriesByType[tenant.vendor.type]
            ?: throw IllegalStateException("No VendorFactory registered for ${tenant.vendor.type}")
}
