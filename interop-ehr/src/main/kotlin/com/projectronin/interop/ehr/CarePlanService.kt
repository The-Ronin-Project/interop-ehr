package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality for an EHR's CarePlan service.
 */
interface CarePlanService : FHIRService<CarePlan> {
    /**
     * Finds a list of patient [CarePlan]s for a date range.
     */
    fun findPatientCarePlans(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ):
        List<CarePlan>
}
