package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality for an EHR's Encounter service.
 */
interface EncounterService : FHIRService<Encounter> {
    /**
     * Finds a list of patient [Encounter]s for a date range.
     */
    fun findPatientEncounters(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ):
        List<Encounter>
}
