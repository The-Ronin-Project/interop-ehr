package com.projectronin.interop.ehr.model

import com.projectronin.interop.common.resource.ResourceType

/**
 * A container for a collection of one or more resources.
 */
interface Bundle<out R : EHRResource> : EHRResource {
    /**
     * The List of resource entries contained within this bundle.
     */
    val resources: List<R>

    /**
     * List of [Link]s to the next, current and previous bundles if available.
     */
    val links: List<Link>

    override val resourceType: ResourceType
        get() = ResourceType.BUNDLE
}
