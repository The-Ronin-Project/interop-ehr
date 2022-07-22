package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Observation
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality of an EHR's observation service.
 */
interface ObservationService {
    /**
     * Finds observations at the requested [tenant],
     * given the list of [patientFhirIds] at the tenant, and the list of [observationCategoryCodes].
     *
     * When entering [observationCategoryCodes],
     * The Observation.category.coding.code may be entered by itself, or the caller may use FHIR token format,
     * to provide both the system and the code, as in:
     * ```
     * system|code
     * ```
     *
     * The FHIR preferred choices for category code values are vital-signs, laboratory, social-history, and others
     * from the system [http://terminology.hl7.org/Codesystem/observation-category](https://terminology.hl7.org/3.1.0/CodeSystem-observation-category.html)
     * The vital-signs code from this system can be input to [observationCategoryCodes] as:
     * ```
     * vital-signs
     * ```
     * but for accuracy, each category code should be input along with its system, as a token:
     * ```
     * http://terminology.hl7.org/Codesystem/observation-category|vital-signs
     * ```
     * A comma-separated list of category tokens needs the system provided with each code,
     * even if codes are in the same system, for example:
     * ```
     * http://terminology.hl7.org/CodeSystem/observation-category|social-history,http://terminology.hl7.org/CodeSystem/observation-category|laboratory
     * ```
     * You may mix tokens and codes in a comma-separated [observationCategoryCodes] list, but a token is always preferred.
     */
    fun findObservationsByPatient(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<String>
    ): Bundle<Observation>
}
