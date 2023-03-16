package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

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
     * [startDate] and [endDate] fields may be ignored if the underlying provider (cerner) does not support them.
     */
    fun getMedicationRequestByPatient(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<MedicationRequest>
}
