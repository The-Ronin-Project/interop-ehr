package com.projectronin.interop.ehr.auth

import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Service capable of providing Authentication.
 */
interface AuthenticationService {
    /**
     * The type of vendor supported by this authentication service.
     */
    val vendorType: VendorType

    /**
     * Retrieves an [Authentication] for the provided [Tenant]. If [disableRetry] is true, then only a single attempt will be made to retrieve an Authentication.
     */
    fun getAuthentication(tenant: Tenant, disableRetry: Boolean = false): Authentication?
}
