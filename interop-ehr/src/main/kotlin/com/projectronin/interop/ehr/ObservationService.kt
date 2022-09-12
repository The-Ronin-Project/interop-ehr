package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality of an EHR's observation service.
 */
interface ObservationService {
    /**
     * Finds the [List] of [Observation]s associated with the requested [tenant], list of [patientFhirId]s,
     * and list of [conditionCategoryCodes].
     * Supports one code or system|value token for category.
     */
    fun findObservationsByPatient(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<String>
    ): List<Observation>

    /**
     * Finds the [List] of [Observation]s associated with the requested [tenant], list of [patientFhirId]s,
     * and list of [conditionCategoryCodes].
     * Supports lists of codes or system|value tokens for category.
     */
    fun findObservationsByPatientAndCategory(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<FHIRSearchToken>
    ):
        List<Observation>
}
