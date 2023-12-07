package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.DiagnosticReportService
import com.projectronin.interop.ehr.cerner.client.CernerClient
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
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CernerDiagnosticReportServiceTest {
    private val cernerClient: CernerClient = mockk()
    private val diagnosticReportService: DiagnosticReportService = CernerDiagnosticReportService(cernerClient)
    private val httpResponse: HttpResponse = mockk()
    private val ehrResponse = EHRResponse(httpResponse, "12345")
    private val validPatientIdBundle = readResource<Bundle>("/ExampleDiagnosticReportPatientIdBundle.json")
    private val validDxReportIdReturn = readResource<DiagnosticReport>("/ExampleDiagnosticReportByIdResponse.json")

    @Test
    fun `getDiagnosticReportById works for DiagnosticReport Id`() {
        val tenant = mockk<Tenant>()
        val mockDiagnosticReport = mockk<DiagnosticReport>(relaxed = true)

        coEvery {
            httpResponse.body<DiagnosticReport>(
                TypeInfo(
                    DiagnosticReport::class,
                    DiagnosticReport::class.java,
                ),
            )
        } returns mockDiagnosticReport
        coEvery { cernerClient.get(tenant, "/DiagnosticReport/fakeFaKEfAKefakE") } returns ehrResponse

        val actual = diagnosticReportService.getByID(tenant, "fakeFaKEfAKefakE")
        assertEquals(mockDiagnosticReport, actual)
    }

    @Test
    fun `getDiagnosticReportById with diagnostic report id returns diagnostic report resource`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )
        val service = mockk<DiagnosticReportService>()
        every {
            service.getByID(
                tenant,
                "fakeFaKEfAKefakE",
            )
        } returns validDxReportIdReturn

        val result = service.getByID(tenant, "fakeFaKEfAKefakE")

        verify { service.getByID(tenant, "fakeFaKEfAKefakE") }

        assertEquals("DiagnosticReport", result.resourceType)
        assertEquals("fakeFaKEfAKefakE", result.id?.value)
        assertEquals("Patient/fakeFaKEfAKefakE", result.subject?.reference?.value)
    }

    @Test
    fun `getDiagnosticReportByPatient returns patient diagnostic request bundle`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientIdBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/DiagnosticReport",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val actual =
            diagnosticReportService.getDiagnosticReportByPatient(
                tenant,
                "fakeFaKEfAKefakE",
            )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, actual)
        assertEquals(1, actual.size)
        actual.forEachIndexed { _, diagnosticRequest ->
            val subject = diagnosticRequest.subject?.reference?.value
            assertTrue(subject == "Patient/fakeFaKEfAKefakE")
        }
    }

    @Test
    fun `getMedicationRequestByPatient succeeds`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientIdBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/DiagnosticReport",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val validResponse =
            diagnosticReportService.getDiagnosticReportByPatient(
                tenant,
                "fakeFaKEfAKefakE",
            )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, validResponse)
    }

    @Test
    fun `getDiagnosticReportByPatient succeeds with date`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientIdBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/DiagnosticReport",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20,
                    "-timing-boundsPeriod" to "ge2023-09-06T00:00:00Z",
                ),
            )
        } returns ehrResponse

        val validResponse =
            diagnosticReportService.getDiagnosticReportByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                LocalDate.of(2023, 9, 6),
            )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, validResponse)
    }
}
