package com.projectronin.interop.tenant.config.model.vendor

import com.fasterxml.jackson.annotation.JsonTypeName
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.AuthenticationConfig

@JsonTypeName("CERNER")
data class Cerner(
    override val instanceName: String,
    override val clientId: String,
    override val authenticationConfig: AuthenticationConfig,
    override val serviceEndpoint: String

) : Vendor {
    override val type: VendorType
        get() = VendorType.CERNER
}
