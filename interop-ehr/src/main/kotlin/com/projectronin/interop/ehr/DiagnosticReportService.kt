package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality for an EHR's Diagnostic Report service.
 */
interface DiagnosticReportService : FHIRService<DiagnosticReport> {

    /**
     * Finds diagnostic report by [patientFhirId]
     */
    fun getDiagnosticReportByPatient(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): List<DiagnosticReport>
}
