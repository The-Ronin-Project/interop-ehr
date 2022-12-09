package com.projectronin.interop.tenant.config.model.vendor

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.AuthenticationConfig

/**
 * Sealed interface for a Vendor. This interface is sealed to prevent external implementations and to allow for easier consumption.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Epic::class, name = "EPIC"),
    JsonSubTypes.Type(value = Cerner::class, name = "CERNER")
)
sealed interface Vendor {
    /**
     * The name of the vendor instance.
     */
    val instanceName: String

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
