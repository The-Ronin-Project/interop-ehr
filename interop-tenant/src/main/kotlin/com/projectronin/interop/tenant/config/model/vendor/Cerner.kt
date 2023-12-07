package com.projectronin.interop.tenant.config.model.vendor

import com.fasterxml.jackson.annotation.JsonTypeName
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.CernerAuthenticationConfig

@JsonTypeName("CERNER")
data class Cerner(
    override val instanceName: String,
    override val clientId: String?,
    override val authenticationConfig: CernerAuthenticationConfig,
    override val serviceEndpoint: String,
    val patientMRNSystem: String,
    val messagePractitioner: String,
    val messageTopic: String?,
    val messageCategory: String?,
    val messagePriority: String?,
) : Vendor {
    override val type: VendorType
        get() = VendorType.CERNER
}
