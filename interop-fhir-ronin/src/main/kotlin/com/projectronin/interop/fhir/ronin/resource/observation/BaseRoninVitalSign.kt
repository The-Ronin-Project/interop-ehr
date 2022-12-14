package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity

/**
 * Base class capable of handling common tasks associated to Ronin Observation profiles.
 */
abstract class BaseRoninVitalSign(
    extendedProfile: ProfileValidator<Observation>,
    profile: String
) : BaseRoninObservation(extendedProfile, profile) {
    internal val vitalSignsCode = Code("vital-signs")

    private val requiredVitalSignsCodeError = FHIRError(
        code = "USCORE_VSOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "A category code of \"${vitalSignsCode.value}\" is required",
        location = LocationContext(Observation::category)
    )

    /**
     * Validates the [element] against Ronin rules for vital sign Observations.
     */
    fun validateVitalSign(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(
                element.category.flatMap { it.coding }
                    .any { it.system == CodeSystem.OBSERVATION_CATEGORY.uri && it.code == vitalSignsCode },
                requiredVitalSignsCodeError,
                parentContext
            )
        }
    }

    /**
     * Validates the Observation against rules from Ronin, USCore, specific Observation type, and vital signs.
     */
    override fun validate(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validate(element, parentContext, validation)
        validateVitalSign(element, parentContext, validation)
    }
}
