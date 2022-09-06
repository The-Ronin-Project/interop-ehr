package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.Validatable
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation

/**
 * Base class supporting validation. If supplied with an optional [extendedProfile], this validator will also validate against it.
 */
abstract class BaseValidator<T : Validatable<T>>(
    private val extendedProfile: ProfileValidator<T>?,
) :
    ProfileValidator<T> {
    /**
     * Validates the [element] on the [validation] using the [parentContext].
     */
    abstract fun validate(element: T, parentContext: LocationContext, validation: Validation)

    override fun validate(element: T, parentContext: LocationContext?): Validation = validation {
        val currentContext = parentContext ?: LocationContext(element::class)

        validate(element, currentContext, this)

        extendedProfile?.let {
            merge(element.validate(it, currentContext))
        }
    }
}
