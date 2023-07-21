package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.MedicationRequestService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Service providing access to Medication Request within Cerner.
 */
@Component
class CernerMedicationRequestService(
    cernerClient: CernerClient
) : MedicationRequestService, CernerFHIRService<MedicationRequest>(cernerClient) {
    override val fhirURLSearchPart = "/MedicationRequest"
    override val fhirResourceType = MedicationRequest::class.java

    @Deprecated("Use getByID")
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
