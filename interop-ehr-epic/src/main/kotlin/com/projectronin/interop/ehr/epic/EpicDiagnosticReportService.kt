package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.DiagnosticReportService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to Diagnostic Report within Epic.
 */
@Component
class EpicDiagnosticReportService(
    epicClient: EpicClient
) : DiagnosticReportService, EpicFHIRService<DiagnosticReport>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/DiagnosticReport"
    override val fhirResourceType = DiagnosticReport::class.java

    override fun getDiagnosticReportByPatient(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<DiagnosticReport> {
        val dateMap = getDateParam(startDate, endDate)?.let { mapOf("date" to it) }
            ?: emptyMap()
        val parameters = mapOf(
            "patient" to patientFhirId
        ) + dateMap
        return getResourceListFromSearch(tenant, parameters)
    }
}
