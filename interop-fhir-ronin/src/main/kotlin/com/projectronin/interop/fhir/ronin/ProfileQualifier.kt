package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.resource.Resource

/**
 * ProfileQualifier enables a MultipleProfileResource (Observation, Condition,
 * DiagnosticReport, etc.) to be tested to determine which RCDM profile it fits.
 */
interface ProfileQualifier<T : Resource<T>> {
    /**
     * Returns true if [resource] qualifies for this particular profile.
     */
    fun qualifies(resource: T): Boolean
}
