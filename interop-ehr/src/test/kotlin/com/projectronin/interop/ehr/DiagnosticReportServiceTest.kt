package com.projectronin.interop.ehr

import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiagnosticReportServiceTest {
    abstract class DiagnosticReportServiceMock : DiagnosticReportService

    private val diagnosticReportService = spyk<DiagnosticReportServiceMock>()

    @Test
    fun `getDiagnosticReportByPatient defaults dates`() {
        val tenant = mockk<Tenant>()
        val patientFhirIds = "fhirId"

        every {
            diagnosticReportService.getDiagnosticReportByPatient(any(), any(), null, null)
        } returns emptyList()

        val results = diagnosticReportService.getDiagnosticReportByPatient(tenant, patientFhirIds)

        assertEquals(0, results.size)
    }
}
