package com.projectronin.interop.ehr

import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MedicationRequestServiceTest {
    abstract class MedicationRequestServiceMock : MedicationRequestService

    private val medicationRequest = spyk<MedicationRequestServiceMock>()

    @Test
    fun `getMedicationRequestByPatient default dates`() {
        val tenant = mockk<Tenant>()
        val patientFhirIds = "fhirId"

        every {
            medicationRequest.getMedicationRequestByPatient(any(), any(), null, null)
        } returns emptyList()

        val results = medicationRequest.getMedicationRequestByPatient(tenant, patientFhirIds)

        assertEquals(0, results.size)
    }
}
