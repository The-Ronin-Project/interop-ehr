package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality for an EHR's Procedure service.
 */
interface ProcedureService : FHIRService<Procedure> {
    /**
     * Finds procedures by [patientFhirId]
     * */
    fun getProcedureByPatient(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Procedure>
}
