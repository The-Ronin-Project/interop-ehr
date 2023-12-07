package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality of an EHR's MedicationStatement service.
 */
interface MedicationStatementService : FHIRService<MedicationStatement> {
    /**
     * Finds the [MedicationStatement] associated with the requested [tenant] and [patientFHIRId].
     */
    fun getMedicationStatementsByPatientFHIRId(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): List<MedicationStatement>
}
