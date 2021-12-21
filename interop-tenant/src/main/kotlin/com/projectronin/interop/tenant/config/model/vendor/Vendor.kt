package com.projectronin.interop.tenant.config.model.vendor

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.AuthenticationConfig

/**
 * Sealed interface for a Vendor. This interface is sealed to prevent external implementations and to allow for easier consumption.
 */
sealed interface Vendor {
    /**
     * The type of vendor.
     */
    val type: VendorType

    /**
     * The client ID associated to this vendor.
     */
    val clientId: String

    /**
     * The authentication configuration for this vendor.
     */
    val authenticationConfig: AuthenticationConfig

    /**
     * The main service endpoint to use for calling this vendor.
     */
    val serviceEndpoint: String
}
