package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality of an EHR's practitioner service.
 */
interface PractitionerService : FHIRService<Practitioner> {
    /**
     * Retrieves the Practitioner associated to the requested [practitionerFHIRId] at [tenant].
     */
    fun getPractitioner(
        tenant: Tenant,
        practitionerFHIRId: String,
    ): Practitioner

    /**
     * Retrieves the Practitioner associated to the requested [providerId] at [tenant]. The provider ID here should match the definition used by [IdentifierService.getPractitionerProviderIdentifier].
     */
    fun getPractitionerByProvider(
        tenant: Tenant,
        providerId: String,
    ): Practitioner

    /**
     * Finds the practitioners associated to the requested [tenant] and [FHIR location IDs][locationIds].
     */
    fun findPractitionersByLocation(
        tenant: Tenant,
        locationIds: List<String>,
    ): FindPractitionersResponse
}
