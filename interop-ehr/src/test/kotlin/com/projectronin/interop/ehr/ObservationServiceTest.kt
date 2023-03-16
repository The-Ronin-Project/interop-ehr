package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ObservationServiceTest {
    // Ideally we'd just mockk the ObservationService interface, but mockk has some issues with that
    // approach.  https://github.com/mockk/mockk/issues/64
    abstract class ObservationServiceMock : ObservationService
    private val observationService = spyk<ObservationServiceMock>()

    @Test
    fun `findObservationsByPatient calls findObservationsByPatientAndCategory`() {
        val tenant = mockk<Tenant>()
        val patientFhirIds = listOf("fhirId")
        val observation = mockk<Observation>()

        every {
            observationService.findObservationsByPatientAndCategory(any(), any(), any())
        } returns listOf(observation)

        val results = observationService.findObservationsByPatient(tenant, patientFhirIds, listOf("code"))

        assertEquals(1, results.size)
        assertSame(observation, results[0])
    }
}
