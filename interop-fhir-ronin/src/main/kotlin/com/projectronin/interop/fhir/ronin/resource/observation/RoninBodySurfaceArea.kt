package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import org.springframework.stereotype.Component

@Component
class RoninBodySurfaceArea(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) :
    BaseRoninVitalSign(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_BODY_SURFACE_AREA.value,
        normalizer,
        localizer,
        registryClient
    ) {
    override val rcdmVersion = RCDMVersion.V3_26_1
    override val profileVersion = 3

    // Quantity unit codes
    override val validQuantityCodes = listOf("m2")

    override fun validateVitalSign(element: Observation, parentContext: LocationContext, validation: Validation) {
        validateVitalSignValue(element.value, parentContext, validation)
    }
}
