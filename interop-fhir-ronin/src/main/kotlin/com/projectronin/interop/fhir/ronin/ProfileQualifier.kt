package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.resource.Resource

/**
 * ProfileQualifier enables a MultipleProfileResource (Observation, Condition,
 * DiagnosticReport, etc.) to be tested to determine which RCDM profile it fits.
 */
interface ProfileQualifier<T : Resource<T>> {
    /**
     * Returns true if [resource] qualifies for this particular profile.
     * [resource] must be an already-transformed MultipleProfileResource.
     *
     * An InterOps channel that gets a resource immediately transforms it, using
     * concept maps for that tenant, and then validates the transformed resource.
     * MultipleProfileResource validate() calls getQualifiedProfile() to see
     * which profile to use. getQualifiedProfile() calls qualifies() on each
     * profile in turn, to determine the answer. This is the only call to
     * qualifies() in InterOps code. It is certain the resource is transformed.
     */
    fun qualifies(resource: T): Boolean
}
