package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.EHRResource
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Transforms all of the [EHRResource]s within this bundle into the request type of [Resource] for the [tenant] using the [transformer].
 */
fun <R : EHRResource, T : Resource> Bundle<R>.transformResources(
    tenant: Tenant,
    transformer: (R, Tenant) -> T?
): List<T> =
    this.resources.mapNotNull { transformer(it, tenant) }
