package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.ObservationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.ehr.util.daysToPastDate
import com.projectronin.interop.ehr.util.toOrParams
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.ObservationCategoryCodes
import com.projectronin.interop.tenant.config.data.TenantCodesDAO
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class CernerObservationService(
    cernerClient: CernerClient,
    private val tenantCodesDAO: TenantCodesDAO,
    @Value("\${cerner.fhir.observation.incrementalLoadDays:60}") private val incrementalLoadDays: Int
) : ObservationService,
    CernerFHIRService<Observation>(cernerClient) {
    override val fhirURLSearchPart = "/Observation"
    override val fhirResourceType = Observation::class.java
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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
                "category" to observationCategoryCodes.toOrParams(),
                "date" to "ge${daysToPastDate(incrementalLoadDays, dateFormat)}"
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
                "category" to observationCategoryCodes.joinToString(separator = ",") { it.code },
                "date" to "ge${daysToPastDate(incrementalLoadDays, dateFormat)}"
            )
            observationResponse.add(getResourceListFromSearch(tenant, parameters))
            if (extraCodesToSearch.isNotEmpty()) {
                parameters = mapOf(
                    "patient" to patientId,
                    "code" to extraCodesToSearch.joinToString(separator = ","),
                    "date" to "ge${daysToPastDate(incrementalLoadDays, dateFormat)}"
                )
                observationResponse.add(getResourceListFromSearch(tenant, parameters))
            }
        }

        return observationResponse.flatten()
    }
}
