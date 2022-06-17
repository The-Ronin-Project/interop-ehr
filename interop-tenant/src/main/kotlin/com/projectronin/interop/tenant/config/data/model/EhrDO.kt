package com.projectronin.interop.tenant.config.data.model

import com.projectronin.interop.common.vendor.VendorType
import org.ktorm.entity.Entity

/**
 * Entity definition for the EHR data object.
 */
interface EhrDO : Entity<EhrDO> {
    companion object : Entity.Factory<EhrDO>()
    var id: Int
    var instanceName: String
    var vendorType: VendorType
    var clientId: String
    var publicKey: String
    var privateKey: String
}
