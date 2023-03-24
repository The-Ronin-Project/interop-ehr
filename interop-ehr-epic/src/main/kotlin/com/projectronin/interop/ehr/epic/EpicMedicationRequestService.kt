package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.MedicationRequestService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

/**
 * Service providing access to MedicationRequestService within Epic.
 */
@Component
class EpicMedicationRequestService(
    epicClient: EpicClient
) : MedicationRequestService, EpicFHIRService<MedicationRequest>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/MedicationRequest"
    override val fhirResourceType = MedicationRequest::class.java

    @Trace
    override fun getMedicationRequestById(
        tenant: Tenant,
        medicationRequestId: String
    ): MedicationRequest {
        return getByID(tenant, medicationRequestId)
    }

    override fun getMedicationRequestByPatient(
        tenant: Tenant,
        patientFhirId: String
    ): List<MedicationRequest> {
        val parameters = mapOf(
            "patient" to patientFhirId
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}
