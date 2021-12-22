package com.projectronin.interop.tenant.config.model.vendor

import com.fasterxml.jackson.annotation.JsonTypeName
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.AuthenticationConfig

/**
 * Epic vendor implementation.
 * @property release The release of Epic being used by the tenant.
 * @property ehrUserId The ID of the User under which Epic communication should occur.
 * @property messageType The type of InBasket message that should be sent to Epic.
 */
@JsonTypeName("EPIC")
data class Epic(
    override val clientId: String,
    override val authenticationConfig: AuthenticationConfig,
    override val serviceEndpoint: String,
    val release: String,
    val ehrUserId: String,
    val messageType: String
) : Vendor {
    override val type: VendorType
        get() = VendorType.EPIC
}
