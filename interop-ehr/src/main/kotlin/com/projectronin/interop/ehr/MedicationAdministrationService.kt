package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality of an EHR's MedicationAdministration service.
 */
interface MedicationAdministrationService : FHIRService<MedicationAdministration> {
    /**
     * Retrieves the MedicationAdministration associated to the requested [medicationRequest] at [tenant].
     */
    fun findMedicationAdministrationsByRequest(
        tenant: Tenant,
        medicationRequest: MedicationRequest
    ): List<MedicationAdministration>

    /**
     * Finds the MedicationAdministrations associated to the requested [tenant] and [patientFHIRId].
     * Requires a start and end [LocalDate]
     */
    fun findMedicationAdministrationsByPatient(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<MedicationAdministration>
}
