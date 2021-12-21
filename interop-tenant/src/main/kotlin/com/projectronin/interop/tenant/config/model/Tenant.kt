package com.projectronin.interop.tenant.config.model

import com.projectronin.interop.tenant.config.model.vendor.Vendor

/**
 * Configuration associated to a Tenant.
 * @property mnemonic The tenant's mnemonic
 * @property batchConfig The batch configuration.
 * @property vendor The vendor-specific configuration.
 */
data class Tenant(
    val mnemonic: String,
    val batchConfig: BatchConfig?,
    val vendor: Vendor
)
