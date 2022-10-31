package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant

interface FHIRService<T : Resource<T>> {
    val fhirResourceType: Class<T>
    fun getByID(tenant: Tenant, resourceFHIRId: String): T
}
