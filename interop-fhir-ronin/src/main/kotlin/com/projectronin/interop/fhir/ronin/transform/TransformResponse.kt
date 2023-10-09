package com.projectronin.interop.fhir.ronin.transform

import com.projectronin.interop.fhir.r4.resource.Resource

/**
 * Response from performing a transform operation including the transformed [resource] and any [embeddedResources] discovered or created during the transformation.
 */
data class TransformResponse<T : Resource<T>>(
    val resource: T,
    val embeddedResources: List<Resource<*>> = emptyList()
)
