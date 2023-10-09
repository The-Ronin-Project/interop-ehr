package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.qualifiesForValueSet
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import org.springframework.stereotype.Component

@Component
class RoninStagingRelated(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) :
    BaseRoninObservation(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_STAGING_RELATED.value,
        normalizer,
        localizer,
        registryClient
    ) {
    override val rcdmVersion = RCDMVersion.V3_26_1
    override val profileVersion = 3

    // category and category.coding must be present but are not fixed values
    // code.coding must exist in the valueSet
    override fun qualifies(resource: Observation): Boolean {
        return (
            (resource.code?.qualifiesForValueSet(qualifyingCodes().codes) == true) &&
                resource.category.isNotEmpty() && resource.category.any { category -> category.coding.isNotEmpty() }
            )
    }

    override fun validateSpecificObservation(
        element: Observation,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            element.code?.coding?.let {
                checkTrue(
                    it.size == 1,
                    FHIRError(
                        code = "RONIN_STAGING_OBS_001",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Coding list must contain exactly 1 entry",
                        location = LocationContext(Observation::code),
                        metadata = listOf(qualifyingCodes().metadata!!)
                    ),
                    parentContext
                )
            }

            checkTrue(
                element.category.any { category -> category.coding.isNotEmpty() },
                FHIRError(
                    code = "RONIN_STAGING_OBS_002",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Coding is required",
                    location = LocationContext(Observation::category),
                    metadata = listOf(qualifyingCodes().metadata!!)
                ),
                parentContext
            )
        }
    }
}
