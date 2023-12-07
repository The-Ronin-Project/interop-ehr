package com.projectronin.interop.ehr

import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ProcedureServiceTest {
    abstract class ProcedureServiceMock : ProcedureService

    private val procedureService = spyk<ProcedureServiceMock>()
    private val startDate = mockk<LocalDate>()
    private val endDate = mockk<LocalDate>()

    @Test
    fun `getProcedureByPatient defaults dates`() {
        val tenant = mockk<Tenant>()
        val patientFhirId = "fhirId"

        every {
            procedureService.getProcedureByPatient(any(), any(), any(), any())
        } returns emptyList()

        val results = procedureService.getProcedureByPatient(tenant, patientFhirId, startDate, endDate)
        assertEquals(0, results.size)
    }
}
