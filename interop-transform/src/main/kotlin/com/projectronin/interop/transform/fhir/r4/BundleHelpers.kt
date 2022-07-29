package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.EHRResource
import com.projectronin.interop.fhir.FHIRResource
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Transforms all of the [EHRResource]s within this bundle into the request type of [FHIRResource] for the [tenant] using the [transformer].
 */
fun <R : EHRResource, T : FHIRResource> Bundle<R>.transformResources(
    tenant: Tenant,
    transformer: (R, Tenant) -> T?
): List<T> =
    this.resources.mapNotNull { transformer(it, tenant) }

// Just a step-gap to prevent broken code
fun <R : EHRResource, T : Resource<T>> Bundle<R>.transformNonRoninResources(
    tenant: Tenant,
    transformer: (R, Tenant) -> T?
): List<T> =
    this.resources.mapNotNull { transformer(it, tenant) }
