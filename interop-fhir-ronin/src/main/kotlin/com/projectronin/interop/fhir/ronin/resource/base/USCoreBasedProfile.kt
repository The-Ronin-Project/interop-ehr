package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.Validation

/**
 * Base class capable of handling common tasks associated to Ronin profiles.
 */
abstract class USCoreBasedProfile<T : Resource<T>>(
    extendedProfile: ProfileValidator<T>,
    profile: String,
    normalizer: Normalizer,
    localizer: Localizer
) :
    BaseRoninProfile<T>(extendedProfile, profile, normalizer, localizer) {
    /**
     * Validates the [element] against Ronin's rules.
     */
    abstract fun validateRonin(element: T, parentContext: LocationContext, validation: Validation)

    /**
     * Validates the [element] against US Core's rules.
     */
    abstract fun validateUSCore(element: T, parentContext: LocationContext, validation: Validation)

    /**
     * Validates both Ronin and USCore against their respective rules.
     */
    override fun validate(element: T, parentContext: LocationContext, validation: Validation) {
        validateRonin(element, parentContext, validation)
        validateUSCore(element, parentContext, validation)
    }
}
