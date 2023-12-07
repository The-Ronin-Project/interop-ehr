package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.ProcedureService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.reflect.TypeInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CernerProcedureServiceTest {
    private val cernerClient: CernerClient = mockk()
    private val procedureService: ProcedureService = CernerProcedureService(cernerClient)
    private val httpResponse: HttpResponse = mockk()
    private val ehrResponse = EHRResponse(httpResponse, "fakeFaKEfAKefakE")

    @Test
    fun `getProcedureById works for procedure Id`() {
        val tenant = mockk<Tenant>()
        val mockProcedure = mockk<Procedure>(relaxed = true)

        coEvery {
            httpResponse.body<Procedure>(
                TypeInfo(
                    Procedure::class,
                    Procedure::class.java,
                ),
            )
        } returns mockProcedure
        coEvery { cernerClient.get(tenant, "/Procedure/fakeFaKEfAKefakE") } returns ehrResponse

        val actual = procedureService.getByID(tenant, "fakeFaKEfAKefakE")
        assertEquals(mockProcedure, actual)
    }

    @Test
    fun `getProcedureByPatient succeeds with date`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                timezone = "UTC-06:00",
            )

        val procedure1 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<Procedure>(relaxed = true) {
                        every { id!!.value } returns "fakeFaKEfAKefakE"
                    }
            }
        val procedure2 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<Procedure>(relaxed = true) {
                        every { id!!.value } returns "fakeFaKEfAKefakE"
                    }
            }
        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(procedure1, procedure2)
                every { link } returns emptyList()
            }
        coEvery {
            cernerClient.get(
                tenant,
                "/Procedure",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "date" to RepeatingParameter(listOf("ge2015-01-01T00:00:00-06:00", "le2015-11-02T00:00:00-06:00")),
                    "_count" to 20,
                ),
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "fakeFaKEfAKefakE")

        val validResponse =
            procedureService.getProcedureByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1),
            )

        assertEquals(listOf(procedure1.resource, procedure2.resource), validResponse)
    }
}
