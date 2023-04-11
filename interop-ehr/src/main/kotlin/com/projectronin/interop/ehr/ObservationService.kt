package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.ehr.util.toSearchTokens
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.ObservationCategoryCodes
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace

/**
 * Defines the functionality of an EHR's observation service.
 */
interface ObservationService : FHIRService<Observation> {
    /**
     * Finds the [List] of [Observation]s associated with the requested [tenant], list of [patientFhirId]s,
     * and list of [conditionCategoryCodes].
     * Supports one code or system|value token for category.
     *
     * The Observation.category.coding.code may be entered by itself, or the caller may use FHIR token format,
     * to provide both the system and the code, as in:
     * ```
     * system|code
     * ```
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
    @Trace
    fun findObservationsByPatient(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<String>
    ): List<Observation> {
        return findObservationsByPatientAndCategory(
            tenant,
            patientFhirIds,
            observationCategoryCodes.toSearchTokens()
        )
    }

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

    fun findObservationsByCategory(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<ObservationCategoryCodes>
    ):
        List<Observation>
}
