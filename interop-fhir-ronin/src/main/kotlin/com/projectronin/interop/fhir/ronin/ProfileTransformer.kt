package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Classes that inherit from this interface are capable of transforming a [Resource] into a specific profile mandated by the class.
 */
interface ProfileTransformer<T : Resource<T>> {
    /**
     * Transforms the [original] resource into a new one for the [tenant] matching this profile. The response includes the
     * transformed resource, if it was transformed without validation errors, and the result of running validation against the transformed resource.
     */
    fun transform(original: T, tenant: Tenant): Pair<T?, Validation>
}
