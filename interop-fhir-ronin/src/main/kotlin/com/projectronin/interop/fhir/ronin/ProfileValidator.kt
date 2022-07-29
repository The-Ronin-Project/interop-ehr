package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.resource.Resource

/**
 * Classes that inherit from this interface are capable of validating a [Resource] against a specific profile mandated by the class.
 */
interface ProfileValidator<T : Resource<T>> {
    /**
     * Validates the [resource] against this profile.
     */
    fun validate(resource: T)
}
