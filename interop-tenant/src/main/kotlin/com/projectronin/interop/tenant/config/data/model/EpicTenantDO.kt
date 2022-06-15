package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity

/**
 * Entity definition for the Epic-specific tenant data object.
 */
interface EpicTenantDO : EHRTenantDO, Entity<EpicTenantDO> {
    companion object : Entity.Factory<EpicTenantDO>()

    var release: String
    var serviceEndpoint: String
    var authEndpoint: String
    var ehrUserId: String
    var messageType: String
    var practitionerProviderSystem: String
    var practitionerUserSystem: String
    var patientMRNSystem: String
    var patientInternalSystem: String
    var hsi: String?
}
