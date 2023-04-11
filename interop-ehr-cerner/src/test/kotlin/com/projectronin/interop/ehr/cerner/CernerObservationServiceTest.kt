package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.ObservationCategoryCodes
import com.projectronin.interop.tenant.config.data.TenantCodesDAO
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CernerObservationServiceTest {
    private val client = mockk<CernerClient>()
    private val codesDAO = mockk<TenantCodesDAO>()
    private val cernerObservationService = spyk(CernerObservationService(client, codesDAO))
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "ronin"
    }

    private val observation = mockk<Observation> {
        every { id } returns mockk {
            every { value } returns "observation"
        }
    }

    private val observation2 = mockk<Observation> {
        every { id } returns mockk {
            every { value } returns "observation2"
        }
    }

    @Test
    fun `findObservationsByPatientAndCategory works`() {
        every {
            cernerObservationService.getResourceListFromSearch(
                tenant,
                mapOf(
                    "patient" to "fhirId",
                    "category" to "code"
                )
            )
        } returns listOf(observation)

        val results = cernerObservationService.findObservationsByPatientAndCategory(
            tenant,
            listOf("fhirId"),
            listOf(FHIRSearchToken(system = null, code = "code"))
        )

        assertEquals(1, results.size)
        assertEquals(observation, results[0])
    }

    @Test
    fun `findObservationsByPatientAndCategory handles multiple categories`() {
        every {
            cernerObservationService.getResourceListFromSearch(
                tenant,
                mapOf(
                    "patient" to "fhirId",
                    "category" to "code,system2|code2"
                )
            )
        } returns listOf(observation)

        val results = cernerObservationService.findObservationsByPatientAndCategory(
            tenant,
            listOf("fhirId"),
            listOf(
                FHIRSearchToken(system = null, code = "code"),
                FHIRSearchToken(system = "system2", code = "code2")
            )
        )

        assertEquals(1, results.size)
        assertEquals(observation, results[0])
    }

    @Test
    fun `findObservationsByCategory handles multiple categories`() {
        every {
            cernerObservationService.getResourceListFromSearch(
                tenant,
                mapOf(
                    "patient" to "fhirId",
                    "category" to "exam,laboratory"
                )
            )
        } returns listOf(observation)

        val results = cernerObservationService.findObservationsByCategory(
            tenant,
            listOf("fhirId"),
            listOf(
                ObservationCategoryCodes.EXAM,
                ObservationCategoryCodes.LABORATORY
            )
        )

        assertEquals(1, results.size)
        assertEquals(observation, results[0])
    }

    @Test
    fun `findObservationsByCategory handles multiple categories and extra codes`() {
        every {
            cernerObservationService.getResourceListFromSearch(
                tenant,
                mapOf(
                    "patient" to "fhirId",
                    "category" to "vital-signs,laboratory"
                )
            )
        } returns listOf(observation)

        every {
            cernerObservationService.getResourceListFromSearch(
                tenant,
                mapOf(
                    "patient" to "fhirId",
                    "code" to "12345,09876"
                )
            )
        } returns listOf(observation2)

        every {
            codesDAO.getByTenantMnemonic("ronin")
        } returns mockk {
            every { bmiCode } returns "12345"
            every { bsaCode } returns "09876"
        }

        val results = cernerObservationService.findObservationsByCategory(
            tenant,
            listOf("fhirId"),
            listOf(
                ObservationCategoryCodes.VITAL_SIGNS,
                ObservationCategoryCodes.LABORATORY
            )
        )

        assertEquals(2, results.size)
        assertEquals(observation, results[0])
        assertEquals(observation2, results[1])
    }

    @Test
    fun `findObservationsByPatientAndCategory handles multiple patients`() {
        every {
            cernerObservationService.getResourceListFromSearch(
                tenant,
                mapOf(
                    "patient" to "fhirId1",
                    "category" to "code"
                )
            )
        } returns listOf(observation)

        every {
            cernerObservationService.getResourceListFromSearch(
                tenant,
                mapOf(
                    "patient" to "fhirId2",
                    "category" to "code"
                )
            )
        } returns listOf(observation2)

        val results = cernerObservationService.findObservationsByPatientAndCategory(
            tenant,
            listOf("fhirId1", "fhirId2"),
            listOf(FHIRSearchToken(system = null, code = "code"))
        )

        assertEquals(2, results.size)
        assertTrue(results.contains(observation))
        assertTrue(results.contains(observation2))
    }

    // Strictly to increase code coverage
    @Test
    fun `can load fhirURLSearchPart and fhirResourceType`() {
        val fhirURLSearchPart = cernerObservationService.fhirURLSearchPart
        val fhirResourceType = cernerObservationService.fhirResourceType
    }
}
