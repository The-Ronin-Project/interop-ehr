package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity

/**
 * Entity definition for the Epic-specific tenant data object.
 */
interface EpicTenantDO : EHRTenantDO, Entity<EpicTenantDO> {
    val release: String
    val serviceEndpoint: String
    val ehrUserId: String
    val messageType: String
    val practitionerProviderSystem: String
    val practitionerUserSystem: String
    val mrnSystem: String
    val hsi: String?
}
