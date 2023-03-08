package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * Table binding definition for [EpicTenantDO] data objects.
 */
object EpicTenantDOs : Table<EpicTenantDO>("io_tenant_epic") {
    var tenantId = int("io_tenant_id").bindTo { it.tenantId }
    val release = varchar("release_version").bindTo { it.release }
    val serviceEndpoint = varchar("service_endpoint").bindTo { it.serviceEndpoint }
    val authEndpoint = varchar("auth_endpoint").bindTo { it.authEndpoint }
    val ehrUserId = varchar("ehr_user_id").bindTo { it.ehrUserId }
    val messageType = varchar("message_type").bindTo { it.messageType }
    val practitionerProviderSystem =
        varchar("practitioner_provider_system").bindTo { it.practitionerProviderSystem }
    val practitionerUserSystem = varchar("practitioner_user_system").bindTo { it.practitionerUserSystem }
    val patientMRNSystem = varchar("mrn_system").bindTo { it.patientMRNSystem }
    val patientInternalSystem = varchar("patient_internal_system").bindTo { it.patientInternalSystem }
    val encounterCSNSystem = varchar("encounter_csn_system").bindTo { it.encounterCSNSystem }
    val patientMRNTypeText = varchar("mrn_type_text").bindTo { it.patientMRNTypeText }
    val hsi = varchar("hsi").bindTo { it.hsi }
    val departmentInternalSystem = varchar("department_internal_system").bindTo { it.departmentInternalSystem }
    val patientOnboardedFlagId = varchar("patient_onboarded_flag_id").bindTo { it.patientOnboardedFlagId }
}
