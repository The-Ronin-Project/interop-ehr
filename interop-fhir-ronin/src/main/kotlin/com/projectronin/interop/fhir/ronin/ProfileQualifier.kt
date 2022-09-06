package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.resource.Resource

/**
 * A ProfileQualifier indicates that a resource can be tested to see if it qualifies for a given profile.
 * In general, qualification should be unnecessary; however, there are cases where multiple profiles may exist
 * for a specific resource type, and ProfileQualifier can help determine which qualify for the specific resource
 * being processed.
 */
interface ProfileQualifier<T : Resource<T>> {
    /**
     * Returns true if [resource] qualifies for this particular profile.
     */
    fun qualifies(resource: T): Boolean
}
