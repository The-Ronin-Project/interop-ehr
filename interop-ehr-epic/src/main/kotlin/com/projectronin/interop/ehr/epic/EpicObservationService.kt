package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.ObservationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.ehr.util.toOrParams
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.ObservationCategoryCodes
import com.projectronin.interop.tenant.config.data.TenantCodesDAO
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Service providing access to observations within Epic.
 */
@Component
class EpicObservationService(
    epicClient: EpicClient,
    @Value("\${epic.fhir.observation.batchSize:1}") private val batchSize: Int, // This is currently ignored.  See comment below.
    private val tenantCodesDAO: TenantCodesDAO
) : ObservationService,
    EpicFHIRService<Observation>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Observation"
    override val fhirResourceType = Observation::class.java

    /**
     * Finds the [List] of [Observation]s associated with the requested [tenant], list of [patientFhirId]s,
     * and list of [observationCategoryCodes].
     * Supports lists of codes or system|value tokens for category.
     */
    @Trace
    override fun findObservationsByPatientAndCategory(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<FHIRSearchToken>
    ): List<Observation> {
        // Epic has a problem handling multiple patients in 1 call, so in the meantime force batch size to 1.
        // See https://sherlock.epic.com/default.aspx?view=slg/home#id=6953019&rv=0
        val observationResponses = patientFhirIds.chunked(1) {
            val parameters = mapOf(
                "patient" to it.joinToString(separator = ","),
                "category" to observationCategoryCodes.toOrParams()
            )
            getResourceListFromSearch(tenant, parameters)
        }
        return observationResponses.flatten()
    }

    /**
     * Finds the [List] of [Observation]s associated with the requested [tenant], list of [patientFhirId]s,
     * and list of [observationCategoryCodes].
     */
    @Trace
    override fun findObservationsByCategory(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<ObservationCategoryCodes>
    ): List<Observation> {
        val extraCodesToSearch = mutableListOf<String>().apply {
            if (observationCategoryCodes.contains(ObservationCategoryCodes.VITAL_SIGNS)) {
                tenantCodesDAO.getByTenantMnemonic(tenant.mnemonic)?.let { tenantCodes ->
                    tenantCodes.bmiCode?.let(::add)
                    tenantCodes.bsaCode?.let(::add)
                }
            }
        }

        val observationResponse = mutableListOf<List<Observation>>()
        patientFhirIds.forEach { patientId ->
            var parameters = mapOf(
                "patient" to patientId,
                "category" to observationCategoryCodes.joinToString(separator = ",") { it.code }
            )
            observationResponse.add(getResourceListFromSearch(tenant, parameters))
            if (extraCodesToSearch.isNotEmpty()) {
                parameters = mapOf(
                    "patient" to patientId,
                    "code" to extraCodesToSearch.joinToString(separator = ",")
                )
                observationResponse.add(getResourceListFromSearch(tenant, parameters))
            }
        }

        return observationResponse.flatten()
    }
}
