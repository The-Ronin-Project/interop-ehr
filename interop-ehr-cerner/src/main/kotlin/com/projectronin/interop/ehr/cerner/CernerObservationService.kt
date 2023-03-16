package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.ObservationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.ehr.util.toOrParams
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CernerObservationService(cernerClient: CernerClient) : ObservationService,
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
}
