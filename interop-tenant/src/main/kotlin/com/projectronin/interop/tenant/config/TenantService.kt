package com.projectronin.interop.tenant.config

import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality for accessing a [Tenant]'s configuration.
 */
interface TenantService {
    /**
     * Retrieves the [Tenant] for the supplied [mnemonic]. If none exists, null will be returned.
     */
    fun getTenantForMnemonic(mnemonic: String): Tenant?
}
