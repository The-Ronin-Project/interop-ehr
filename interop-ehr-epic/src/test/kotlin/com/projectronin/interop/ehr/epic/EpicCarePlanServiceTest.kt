package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.CarePlanService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EpicCarePlanServiceTest {
    private var epicClient: EpicClient = mockk()
    private var carePlanService: CarePlanService = EpicCarePlanService(epicClient)

    @Test
    fun getCarePlansByFHIRId() {
        val tenant = mockk<Tenant>()

        val carePlan1 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<CarePlan>(relaxed = true) {
                        every { id!!.value } returns "12345"
                    }
            }
        val carePlan2 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<CarePlan>(relaxed = true) {
                        every { id!!.value } returns "67890"
                    }
            }
        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(carePlan1, carePlan2)
                every { link } returns emptyList()
            }

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/CarePlan",
                mapOf("patient" to "12345", "category" to "736378000", "_count" to 50),
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "12345")

        val response =
            carePlanService.findPatientCarePlans(
                tenant,
                "12345",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1),
            )

        assertEquals(listOf(carePlan1.resource, carePlan2.resource), response)
    }
}
