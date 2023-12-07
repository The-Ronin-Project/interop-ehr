package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.DiagnosticReportService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service providing access to Diagnostic Report within Cerner.
 */
@Component
class CernerDiagnosticReportService(
    cernerClient: CernerClient,
) : DiagnosticReportService, CernerFHIRService<DiagnosticReport>(cernerClient) {
    override val fhirURLSearchPart = "/DiagnosticReport"
    override val fhirResourceType = DiagnosticReport::class.java

    override fun getDiagnosticReportByPatient(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): List<DiagnosticReport> {
        val offset = tenant.timezone.rules.getOffset(LocalDateTime.now())
        val dateMap =
            startDate?.let { mapOf("-timing-boundsPeriod" to "ge${startDate}T00:00:00$offset") }
                ?: emptyMap()
        val parameters =
            mapOf(
                "patient" to patientFhirId,
            ) + dateMap
        return getResourceListFromSearch(tenant, parameters)
    }
}
