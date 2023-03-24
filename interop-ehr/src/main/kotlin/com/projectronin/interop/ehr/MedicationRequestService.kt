package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality of an EHR's medication request service
 */
interface MedicationRequestService : FHIRService<MedicationRequest> {
    /**
     * Finds medication requests by direct [medicationRequestId]
     */
    fun getMedicationRequestById(
        tenant: Tenant,
        medicationRequestId: String
    ): MedicationRequest

    /**
     * Finds medication requests by [patientFhirId]
     */
    fun getMedicationRequestByPatient(
        tenant: Tenant,
        patientFhirId: String
    ): List<MedicationRequest>
}
