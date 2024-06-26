package com.projectronin.interop.tenant.config.data.model

import org.ktorm.entity.Entity

interface CernerTenantDO : EHRTenantDO, Entity<CernerTenantDO> {
    companion object : Entity.Factory<CernerTenantDO>()

    var serviceEndpoint: String
    var patientMRNSystem: String
    var authEndpoint: String
    var messagePractitioner: String
    var messageTopic: String?
    var messageCategory: String?
    var messagePriority: String?
}
