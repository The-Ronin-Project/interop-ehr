package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.EncounterService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.cerner.client.RepeatingParameter
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Encounter
import io.ktor.client.call.body
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CernerEncounterServiceTest {
    private var cernerClient: CernerClient = mockk()
    private var encounterService: EncounterService = CernerEncounterService(cernerClient)

    @Test
    fun getEncountersByFHIRId() {
        val tenant = createTestTenant(
            clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
            authEndpoint = "https://example.org",
            secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            timezone = "UTC-06:00"
        )

        val encounter1 = mockk<BundleEntry> {
            every { resource } returns mockk<Encounter>(relaxed = true) {
                every { id!!.value } returns "12345"
            }
        }
        val encounter2 = mockk<BundleEntry> {
            every { resource } returns mockk<Encounter>(relaxed = true) {
                every { id!!.value } returns "67890"
            }
        }
        val bundle = mockk<Bundle>(relaxed = true) {
            every { entry } returns listOf(encounter1, encounter2)
            every { link } returns emptyList()
        }

        coEvery {
            cernerClient.get(
                tenant,
                "/Encounter",
                mapOf(
                    "patient" to "12345",
                    "date" to RepeatingParameter(listOf("ge2015-01-01T00:00:00-06:00", "lt2015-11-02T00:00:00-06:00")),
                    "_count" to 20
                )
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "12345")

        val response =
            encounterService.findPatientEncounters(
                tenant,
                "12345",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            ) // test de-duplicate

        assertEquals(listOf(encounter1.resource, encounter2.resource), response)
    }
}
