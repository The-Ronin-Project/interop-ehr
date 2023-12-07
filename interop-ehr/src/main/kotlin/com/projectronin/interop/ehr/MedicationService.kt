package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality of an EHR's medication service
 */
interface MedicationService : FHIRService<Medication> {
    /**
     * Finds medications for the given [tenant] associated with the given [medicationFhirIds]
     */
    fun getMedicationsByFhirId(
        tenant: Tenant,
        medicationFhirIds: List<String>,
    ): List<Medication>
}
