package com.projectronin.interop.tenant.config

import com.projectronin.interop.tenant.config.data.model.CernerTenantDO
import com.projectronin.interop.tenant.config.data.model.EHRTenantDO
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import com.projectronin.interop.tenant.config.model.BatchConfig
import com.projectronin.interop.tenant.config.model.CernerAuthenticationConfig
import com.projectronin.interop.tenant.config.model.EpicAuthenticationConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Cerner
import com.projectronin.interop.tenant.config.model.vendor.Epic
import com.projectronin.interop.tenant.config.model.vendor.Vendor

/**
 *  Decompose a [Tenant] into an [TenantDO], link to relevant [ehrDO] required
 */
internal fun Tenant.toTenantDO(ehrDO: EhrDO): TenantDO {
    return TenantDO {
        // these this@ are necessary for kotlin to know we're talking about the value on the original Tenant object
        // and not the TenantDO we're constructing
        mnemonic = this@toTenantDO.mnemonic
        name = this@toTenantDO.name
        ehr = ehrDO
        timezone = this@toTenantDO.timezone
        availableBatchStart = this@toTenantDO.batchConfig?.availableStart
        availableBatchEnd = this@toTenantDO.batchConfig?.availableEnd
    }
}

/**
 *  Transforms an arbitrary [TenantDO] object into a [Tenant] object
 *  [ehrTenantDO] and [ehrDO] are necessary to create the associated vendor
 *  It would be nice to implement a "toVendor" directly on the EHRTenantDO / EpicTenantDO, but a bug in mockk
 *  prevents us from adding functions there and properly testing them
 *  https://github.com/mockk/mockk/issues/64
 */
fun TenantDO.toTenant(ehrTenantDO: EHRTenantDO, ehrDO: EhrDO): Tenant {
    val batchConfig = this.createBatchConfig()
    return Tenant(
        internalId = id,
        mnemonic = mnemonic,
        name = name,
        timezone = timezone,
        batchConfig = batchConfig,
        vendor = ehrTenantDO.toVendor(ehrDO)
    )
}

/**
 *  Transforms the relevant parts of a [TenantDO] object into an optional [BatchConfig] object
 */
private fun TenantDO.createBatchConfig(): BatchConfig? {
    val start = availableBatchStart ?: return null
    val end = availableBatchEnd ?: return null

    return BatchConfig(availableStart = start, availableEnd = end)
}

/**
 *  Transforms an arbitrary [EHRTenantDO] object into an [Vendor] object, provided a link to the ehrDO representing
 *  the vendor
 *  Currently only supports Epic
 *  It would be nice to implement a "toVendor" directly on the EHRTenantDO / EpicTenantDO, but a bug in mockk
 *  prevents us from adding functions there and properly testing them
 *  https://github.com/mockk/mockk/issues/64
 */
fun EHRTenantDO.toVendor(ehrDO: EhrDO): Vendor {
    return when (this) {
        is EpicTenantDO -> this.toEpic(ehrDO)
        is CernerTenantDO -> this.toCerner(ehrDO)
        else -> throw NotImplementedError("Received a ehrDO of unknown subtype")
    }
}

/**
 *  Transforms an [EpicTenantDO] object into an [Epic] object, provided a link to the ehrDO representing Epic
 */
private fun EpicTenantDO.toEpic(ehrDO: EhrDO): Epic {
    val authenticationConfig = EpicAuthenticationConfig(
        authEndpoint = authEndpoint,
        publicKey = ehrDO.publicKey!!,
        privateKey = ehrDO.privateKey!!
    )
    return Epic(
        clientId = ehrDO.clientId,
        instanceName = ehrDO.instanceName,
        authenticationConfig = authenticationConfig,
        serviceEndpoint = serviceEndpoint,
        release = release,
        ehrUserId = ehrUserId,
        messageType = messageType,
        practitionerProviderSystem = practitionerProviderSystem,
        practitionerUserSystem = practitionerUserSystem,
        patientMRNSystem = patientMRNSystem,
        patientInternalSystem = patientInternalSystem,
        encounterCSNSystem = encounterCSNSystem,
        patientMRNTypeText = patientMRNTypeText,
        hsi = hsi,
        departmentInternalSystem = departmentInternalSystem,
    )
}

private fun CernerTenantDO.toCerner(ehrDO: EhrDO): Cerner {
    val authenticationConfig = CernerAuthenticationConfig(
        authEndpoint = authEndpoint,
        accountId = ehrDO.accountId!!,
        secret = ehrDO.secret!!,
    )
    return Cerner(
        clientId = ehrDO.clientId,
        instanceName = ehrDO.instanceName,
        authenticationConfig = authenticationConfig,
        serviceEndpoint = serviceEndpoint,
        patientMRNSystem = patientMRNSystem,
        messagePractitioner = messagePractitioner,
        messageTopic = messageTopic,
        messageCategory = messageCategory,
        messagePriority = messagePriority
    )
}

internal fun Vendor.toEHRTenantDO(tenantID: Int): EHRTenantDO {
    return when (this) {
        is Epic -> this.toEpicTenantDO(tenantID)
        is Cerner -> this.toCernerTenantDO(tenantID)
    }
}

/**
 *  Transforms an [Epic] object into an [EpicTenantDO] object, provided a link to the Tenant ID
 */
private fun Epic.toEpicTenantDO(tenantID: Int): EpicTenantDO {
    return EpicTenantDO {
        tenantId = tenantID
        // these this@ are necessary for kotlin to know we're talking about the value on the original Epic object
        // and not the EpicTenantDO we're constructing
        this.serviceEndpoint = this@toEpicTenantDO.serviceEndpoint
        authEndpoint = this@toEpicTenantDO.authenticationConfig.authEndpoint
        release = this@toEpicTenantDO.release
        ehrUserId = this@toEpicTenantDO.ehrUserId
        messageType = this@toEpicTenantDO.messageType
        practitionerProviderSystem = this@toEpicTenantDO.practitionerProviderSystem
        practitionerUserSystem = this@toEpicTenantDO.practitionerUserSystem
        patientMRNSystem = this@toEpicTenantDO.patientMRNSystem
        patientInternalSystem = this@toEpicTenantDO.patientInternalSystem
        encounterCSNSystem = this@toEpicTenantDO.encounterCSNSystem
        patientMRNTypeText = this@toEpicTenantDO.patientMRNTypeText
        hsi = this@toEpicTenantDO.hsi
        departmentInternalSystem = this@toEpicTenantDO.departmentInternalSystem
    }
}

private fun Cerner.toCernerTenantDO(tenantID: Int): CernerTenantDO {
    return CernerTenantDO {
        tenantId = tenantID
        this.serviceEndpoint = this@toCernerTenantDO.serviceEndpoint
        this.authEndpoint = this@toCernerTenantDO.authenticationConfig.authEndpoint
        patientMRNSystem = this@toCernerTenantDO.patientMRNSystem
        messagePractitioner = this@toCernerTenantDO.messagePractitioner
        messageTopic = this@toCernerTenantDO.messageTopic
        messageCategory = this@toCernerTenantDO.messageCategory
        messagePriority = this@toCernerTenantDO.messagePriority
    }
}
