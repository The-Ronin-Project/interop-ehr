package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.ehr.ProcedureService
import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.util.reflect.TypeInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class EpicProcedureServiceTest {
    private val epicClient: EpicClient = mockk()
    private val procedureService: ProcedureService = EpicProcedureService(epicClient)
    private val httpResponse: HttpResponse = mockk()
    private val ehrResponse = EHRResponse(httpResponse, "12345")

    private val procedureById = readResource<Procedure>("/ExampleProcedureById.json")
    private val procedureByPatient = readResource<Bundle>("/ExampleProcedureByPatient.json")

    @Test
    fun `getProcedureById - works`() {
        val tenant = mockk<Tenant>()
        val procedure = mockk<Procedure>(relaxed = true)

        coEvery {
            httpResponse.body<Procedure>(
                TypeInfo(
                    Procedure::class,
                    Procedure::class.java,
                ),
            )
        } returns procedure
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/Procedure/fakeFaKEfAKefakE") } returns ehrResponse

        val actual = procedureService.getByID(tenant, "fakeFaKEfAKefakE")
        assertEquals(procedure, actual)
    }

    @Test
    fun `getProcedureById throws exception`() {
        val tenant = mockk<Tenant>()
        val thrownException = ClientFailureException(HttpStatusCode.NotFound, "Not Found")
        coEvery {
            httpResponse.body<Procedure>(
                TypeInfo(
                    Procedure::class,
                    Procedure::class.java,
                ),
            )
        } throws thrownException
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/Procedure/fakeFaKEfAKefakE") } returns ehrResponse

        val exception =
            assertThrows<ClientFailureException> {
                procedureService.getByID(
                    tenant,
                    "fakeFaKEfAKefakE",
                )
            }

        assertEquals(thrownException, exception)
    }

    @Test
    fun `getProcedureById returns Procedure resource`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery {
            httpResponse.body<Procedure>(
                TypeInfo(Procedure::class, Procedure::class.java),
            )
        } returns procedureById

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Procedure/fakeFaKEfAKefakE",
            )
        } returns ehrResponse

        val resource =
            procedureService.getByID(
                tenant,
                "fakeFaKEfAKefakE",
            )

        assertEquals(procedureById, resource)
    }

    @Test
    fun `getProcedureByPatient returns patient Procedure bundle`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns procedureByPatient

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Procedure",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "date" to RepeatingParameter(values = listOf("ge2023-09-01", "le2023-09-21")),
                    "_count" to 50,
                ),
            )
        } returns ehrResponse
        val response =
            procedureService.getProcedureByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2023, 9, 21),
            )

        assertEquals(procedureByPatient.entry.map { it.resource }, response)
    }
}
