package com.projectronin.interop.tenant.config.data.model

import com.projectronin.interop.common.vendor.VendorType
import org.ktorm.entity.Entity

/**
 * Entity definition for the EHR data object.
 */
interface EhrDO : Entity<EhrDO> {
    val id: Int
    val vendorType: VendorType
    val clientId: String
    val publicKey: String
    val privateKey: String
}
