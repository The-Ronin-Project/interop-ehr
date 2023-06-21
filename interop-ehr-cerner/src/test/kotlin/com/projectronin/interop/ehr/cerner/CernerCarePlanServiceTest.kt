package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.CarePlanService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.cerner.client.RepeatingParameter
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.CarePlan
import io.ktor.client.call.body
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CernerCarePlanServiceTest {
    private var cernerClient: CernerClient = mockk()
    private var carePlanService: CarePlanService = CernerCarePlanService(cernerClient)

    @Test
    fun getCarePlansByFHIRId() {
        val tenant = createTestTenant(
            clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
            authEndpoint = "https://example.org",
            secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            timezone = "UTC-06:00"
        )

        val carePlan1 = mockk<BundleEntry> {
            every { resource } returns mockk<CarePlan>(relaxed = true) {
                every { id!!.value } returns "12345"
            }
        }
        val carePlan2 = mockk<BundleEntry> {
            every { resource } returns mockk<CarePlan>(relaxed = true) {
                every { id!!.value } returns "67890"
            }
        }
        val bundle = mockk<Bundle>(relaxed = true) {
            every { entry } returns listOf(carePlan1, carePlan2)
            every { link } returns emptyList()
        }

        coEvery {
            cernerClient.get(
                tenant,
                "/CarePlan",
                mapOf(
                    "patient" to "12345",
                    "date" to RepeatingParameter(listOf("ge2015-01-01T00:00:00-06:00", "lt2015-11-02T00:00:00-06:00")),
                    "category" to "assess-plan",
                    "_count" to 20
                )
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "12345")

        val response =
            carePlanService.findPatientCarePlans(
                tenant,
                "12345",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            ) // test de-duplicate

        assertEquals(listOf(carePlan1.resource, carePlan2.resource), response)
    }
}
