package com.projectronin.interop.ehr

import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MedicationStatementServiceTest {
    abstract class MedicationStatementServiceMock : MedicationStatementService

    private val medicationStatement = spyk<MedicationStatementServiceMock>()

    @Test
    fun `getMedicationStatementsByPatientFHIRId defaults dates`() {
        val tenant = mockk<Tenant>()
        val patientFhirIds = "fhirId"

        every {
            medicationStatement.getMedicationStatementsByPatientFHIRId(any(), any(), null, null)
        } returns emptyList()

        val results = medicationStatement.getMedicationStatementsByPatientFHIRId(tenant, patientFhirIds)

        assertEquals(0, results.size)
    }
}
