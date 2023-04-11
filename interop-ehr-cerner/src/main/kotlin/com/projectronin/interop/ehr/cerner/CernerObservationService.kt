package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.ObservationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.ehr.util.toOrParams
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.ObservationCategoryCodes
import com.projectronin.interop.tenant.config.data.TenantCodesDAO
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CernerObservationService(cernerClient: CernerClient, private val tenantCodesDAO: TenantCodesDAO) :
    ObservationService,
    CernerFHIRService<Observation>(cernerClient) {
    override val fhirURLSearchPart = "/Observation"
    override val fhirResourceType = Observation::class.java

    @Trace
    override fun findObservationsByPatientAndCategory(
        tenant: Tenant,
        patientFhirIds: List<String>,
        observationCategoryCodes: List<FHIRSearchToken>
    ): List<Observation> {
        // Cerner doesn't support bundling multiple patients together, so run one patient at a time.
        val observationResponses = patientFhirIds.map {
            val parameters = mapOf(
                "patient" to it,
                "category" to observationCategoryCodes.toOrParams()
            )
            getResourceListFromSearch(tenant, parameters)
        }
        return observationResponses.flatten()
    }

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

        // Cerner doesn't support bundling multiple patients together, so run one patient at a time.
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
