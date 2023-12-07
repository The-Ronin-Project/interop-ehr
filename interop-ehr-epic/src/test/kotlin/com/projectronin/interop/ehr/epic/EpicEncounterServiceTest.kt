package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.EncounterService
import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EpicEncounterServiceTest {
    private var epicClient: EpicClient = mockk()
    private var encounterService: EncounterService = EpicEncounterService(epicClient)

    @Test
    fun getEncountersByFHIRId() {
        val tenant = mockk<Tenant>()

        val encounter1 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<Encounter>(relaxed = true) {
                        every { id!!.value } returns "12345"
                    }
            }
        val encounter2 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<Encounter>(relaxed = true) {
                        every { id!!.value } returns "67890"
                    }
            }
        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(encounter1, encounter2)
                every { link } returns emptyList()
            }

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Encounter",
                mapOf(
                    "patient" to "12345",
                    "date" to RepeatingParameter(listOf("ge2015-01-01", "le2015-11-01")),
                    "_count" to 50,
                ),
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "12345")

        val response =
            encounterService.findPatientEncounters(
                tenant,
                "12345",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1),
            ) // test de-duplicate

        assertEquals(listOf(encounter1.resource, encounter2.resource), response)
    }
}
