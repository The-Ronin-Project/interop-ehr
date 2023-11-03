package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.ehr.DiagnosticReportService
import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
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

class EpicDiagnosticReportServiceTest {
    private val epicClient: EpicClient = mockk()
    private val diagnosticReportService: DiagnosticReportService = EpicDiagnosticReportService(epicClient)
    private val httpResponse: HttpResponse = mockk()
    private val ehrResponse = EHRResponse(httpResponse, "12345")

    private val diagnosticReportById = readResource<DiagnosticReport>("/ExampleDiagnosticReportById.json")
    private val diagnosticReportByPatient = readResource<Bundle>("/ExampleDiagnosticReportByPatient.json")

    @Test
    fun `getDiagnosticReportById - works`() {
        val tenant = mockk<Tenant>()
        val diagnosticReport = mockk<DiagnosticReport>(relaxed = true)

        coEvery {
            httpResponse.body<DiagnosticReport>(
                TypeInfo(
                    DiagnosticReport::class,
                    DiagnosticReport::class.java
                )
            )
        } returns diagnosticReport
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/DiagnosticReport/fakeFaKEfAKefakE") } returns ehrResponse

        val actual = diagnosticReportService.getByID(tenant, "fakeFaKEfAKefakE")
        assertEquals(diagnosticReport, actual)
    }

    @Test
    fun `getDiagnosticReportById throws exception`() {
        val tenant = mockk<Tenant>()
        val thrownException = ClientFailureException(HttpStatusCode.NotFound, "Not Found")
        coEvery {
            httpResponse.body<DiagnosticReport>(
                TypeInfo(
                    DiagnosticReport::class,
                    DiagnosticReport::class.java
                )
            )
        } throws thrownException
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/DiagnosticReport/fakeFaKEfAKefakE") } returns ehrResponse

        val exception =
            assertThrows<ClientFailureException> {
                diagnosticReportService.getByID(
                    tenant,
                    "fakeFaKEfAKefakE"
                )
            }

        assertEquals(thrownException, exception)
    }

    @Test
    fun `getDiagnosticReportById returns Diagnostic Report`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery {
            httpResponse.body<DiagnosticReport>(
                TypeInfo(DiagnosticReport::class, DiagnosticReport::class.java)
            )
        } returns diagnosticReportById

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/DiagnosticReport/fakeFaKEfAKefakE"
            )
        } returns ehrResponse

        val resource =
            diagnosticReportService.getByID(
                tenant,
                "fakeFaKEfAKefakE"
            )

        assertEquals(diagnosticReportById, resource)
    }

    @Test
    fun `getDiagnosticReportByPatient returns patient Diagnostic Report bundle`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns diagnosticReportByPatient

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/DiagnosticReport",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 50
                )
            )
        } returns ehrResponse

        val bundle =
            diagnosticReportService.getDiagnosticReportByPatient(
                tenant,
                "fakeFaKEfAKefakE"
            )

        assertEquals(diagnosticReportByPatient.entry.map { it.resource }, bundle)
    }

    @Test
    fun `getDiagnosticReportByPatient returns patient diagnostic report bundle with dates`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns diagnosticReportByPatient

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/DiagnosticReport",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 50,
                    "date" to RepeatingParameter(values = listOf("ge2023-09-01", "le2023-09-21"))

                )
            )
        } returns ehrResponse

        val bundle =
            diagnosticReportService.getDiagnosticReportByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                LocalDate.of(2023, 9, 1),
                LocalDate.of(2023, 9, 21)
            )

        assertEquals(diagnosticReportByPatient.entry.map { it.resource }, bundle)
    }
}
