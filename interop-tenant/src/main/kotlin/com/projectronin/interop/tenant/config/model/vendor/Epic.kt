package com.projectronin.interop.tenant.config.model.vendor

import com.fasterxml.jackson.annotation.JsonTypeName
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.EpicAuthenticationConfig

/**
 * Epic vendor implementation.
 * @property release The release of Epic being used by the tenant.
 * @property ehrUserId The ID of the User under which Epic communication should occur.
 * @property messageType The type of InBasket message that should be sent to Epic.
 * @property practitionerProviderSystem The system utilized by this Epic instance to represent a practitioner's provider ID.
 * @property practitionerUserSystem The system utilized by this Epic instance to represent a practitioner's user ID.
 * @property patientMRNSystem The system utilized by this Epic instance to represent the MRN we should use as the patient's MRN
 * @property patientInternalSystem The system utilized by this Epic instance to represent the Epic "internal" identifier
 * @property hsi The HSI value to be used for integration with Epic's Tesseract gateway, null if not leveraging Tesseract.
 * @property departmentInternalSystem The system that represent the Epic "internal" department identifier on a Location.
 * @property patientOnboardedFlagId Epic ID for the patient flag that indicates if the patient has onboarded with Ronin.
 */
@JsonTypeName("EPIC")
data class Epic(
    override val clientId: String?,
    override val authenticationConfig: EpicAuthenticationConfig,
    override val serviceEndpoint: String,
    override val instanceName: String,
    val release: String,
    val ehrUserId: String,
    val messageType: String,
    val practitionerProviderSystem: String,
    val practitionerUserSystem: String,
    val patientMRNSystem: String,
    val patientInternalSystem: String,
    val encounterCSNSystem: String,
    val patientMRNTypeText: String,
    val hsi: String? = null,
    val departmentInternalSystem: String,
    val patientOnboardedFlagId: String? = null
) : Vendor {
    override val type: VendorType
        get() = VendorType.EPIC
}
