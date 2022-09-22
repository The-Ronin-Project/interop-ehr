package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Medication
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicMedicationServiceTest {
    private val locationSearchUrlPart = "/api/FHIR/R4/Medication"
    private val epicClient = mockk<EpicClient>()
    private var medicationService = spyk(EpicMedicationService(epicClient, 5))

    private val tenant = createTestTenant()
    private val med1Id = "123"
    private val med1 = mockk<Medication> {
        every { id } returns mockk {
            every { value } returns med1Id
        }
        every { resourceType } returns "Medication"
    }
    private val med2Id = "456"
    private val med2 = mockk<Medication> {
        every { id } returns mockk {
            every { value } returns med2Id
        }
        every { resourceType } returns "Medication"
    }

    @Test
    fun `getMedicationsByFhirId - works with one medication`() {
        val parameters = mapOf("_id" to med1Id)

        val bundle = mockk<Bundle> {
            every { entry } returns listOf(
                mockk { every { resource } returns med1 }
            )
        }

        every { medicationService.getBundleWithPaging(tenant, locationSearchUrlPart, parameters) } returns bundle

        val results = medicationService.getMedicationsByFhirId(tenant, listOf(med1Id))

        assertEquals(1, results.size)
        assertEquals(med1, results[0])
    }

    @Test
    fun `getMedicationsByFhirId - works with multiple medications`() {
        val parameters = mapOf("_id" to "$med1Id,$med2Id")

        val bundle = mockk<Bundle> {
            every { entry } returns listOf(
                mockk { every { resource } returns med1 },
                mockk { every { resource } returns med2 }
            )
        }

        every { medicationService.getBundleWithPaging(tenant, locationSearchUrlPart, parameters) } returns bundle

        val results = medicationService.getMedicationsByFhirId(tenant, listOf(med1Id, med2Id))

        assertEquals(2, results.size)
        assertEquals(med1, results[0])
        assertEquals(med2, results[1])
    }

    @Test
    fun `getMedicationsByFhirId - works with no medications`() {
        val parameters = mapOf("_id" to "")

        val bundle = mockk<Bundle> {
            every { entry } returns listOf()
        }

        every { medicationService.getBundleWithPaging(tenant, locationSearchUrlPart, parameters) } returns bundle

        val results = medicationService.getMedicationsByFhirId(tenant, listOf())

        assertEquals(0, results.size)
    }
}
