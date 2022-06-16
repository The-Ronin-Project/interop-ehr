package com.projectronin.interop.tenant.config.model

import com.projectronin.interop.tenant.config.model.vendor.Vendor

/**
 * Configuration associated to a Tenant.
 * @property internalId The ID of the backing data store for this Tenant.
 * @property mnemonic The tenant's mnemonic.
 * @property name The tenant's full name.
 * @property batchConfig The batch configuration.
 * @property vendor The vendor-specific configuration.
 */
data class Tenant(
    val internalId: Int,
    val mnemonic: String,
    val name: String,
    val batchConfig: BatchConfig?,
    val vendor: Vendor
) {
    inline fun <reified T : Vendor> vendorAs(): T {
        if (vendor !is T) throw RuntimeException("Vendor is not a ${T::class.java.simpleName}")
        return vendor
    }
}
