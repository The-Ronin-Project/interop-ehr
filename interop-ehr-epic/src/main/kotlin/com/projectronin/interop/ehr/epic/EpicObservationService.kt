package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.ObservationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicObservationBundle
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Observation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Service providing access to observations within Epic.
 */
@Component
class EpicObservationService(
    epicClient: EpicClient,
    @Value("\${epic.fhir.observation.batchSize:1}") private val batchSize: Int, // This is currently ignored.  See comment below.
) : ObservationService,
    EpicPagingService(epicClient) {
    private val observationSearchUrlPart = "/api/FHIR/R4/Observation"

    /**
     * Finds observations at the requested [tenant],
     * given the list of [fhirPatientIds] at the tenant, and the list of [observationCategoryCodes].
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
    override fun findObservationsByPatient(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<String>
    ): Bundle<Observation> {
        // Epic has a problem handling multiple patients in 1 call, so in the meantime force batch size to 1.
        // See https://sherlock.epic.com/default.aspx?view=slg/home#id=6953019&rv=0
        val observationResponses = patientFhirIds.chunked(1) {
            val parameters = mapOf(
                "patient" to it.joinToString(separator = ","),
                "category" to observationCategoryCodes.joinToString(separator = ","),
            )
            getBundleWithPaging(tenant, observationSearchUrlPart, parameters, ::EpicObservationBundle)
        }
        return mergeResponses(observationResponses, ::EpicObservationBundle)
    }
}
