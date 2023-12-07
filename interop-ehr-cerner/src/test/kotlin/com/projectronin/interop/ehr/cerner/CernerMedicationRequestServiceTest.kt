package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.MedicationRequestService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
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

class CernerMedicationRequestServiceTest {
    private val cernerClient: CernerClient = mockk()
    private val medicationRequestService: MedicationRequestService = CernerMedicationRequestService(cernerClient)
    private val httpResponse: HttpResponse = mockk()
    private val ehrResponse = EHRResponse(httpResponse, "12345")
    private val validPatientIdBundle = readResource<Bundle>("/ExampleMedicationRequestPatientIdBundle.json")
    private val validMedIdReturn = readResource<MedicationRequest>("/ExampleMedicalRequestByIdResponse.json")

    @Test
    fun `getMedicationRequestById works for medication Id`() {
        val tenant = mockk<Tenant>()
        val mockMedicationRequest = mockk<MedicationRequest>(relaxed = true)

        coEvery {
            httpResponse.body<MedicationRequest>(
                TypeInfo(
                    MedicationRequest::class,
                    MedicationRequest::class.java,
                ),
            )
        } returns mockMedicationRequest
        coEvery { cernerClient.get(tenant, "/MedicationRequest/fakeFaKEfAKefakE") } returns ehrResponse

        val actual = medicationRequestService.getMedicationRequestById(tenant, "fakeFaKEfAKefakE")
        assertEquals(mockMedicationRequest, actual)
    }

    @Test
    fun `getMedicationRequestById with medication request id returns medication request resource`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )
        val service = mockk<MedicationRequestService>()
        every {
            service.getMedicationRequestById(
                tenant,
                "fakeFaKEfAKefakE",
            )
        } returns validMedIdReturn

        val result = service.getMedicationRequestById(tenant, "fakeFaKEfAKefakE")

        verify { service.getMedicationRequestById(tenant, "fakeFaKEfAKefakE") }

        assertEquals("MedicationRequest", result.resourceType)
        assertEquals("fakeFaKEfAKefakE", result.id?.value)
        assertEquals("Patient/fakeFaKEfAKefakE", result.subject?.reference?.value)
    }

    @Test
    fun `getMedicationRequestByPatient returns patient medication request bundle`() {
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
                "/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val actual =
            medicationRequestService.getMedicationRequestByPatient(
                tenant,
                "fakeFaKEfAKefakE",
            )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, actual)
        assertEquals(25, actual.size)
        actual.forEachIndexed { _, medicationRequest ->
            val subject = medicationRequest.subject?.reference?.value
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
                "/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val validResponse =
            medicationRequestService.getMedicationRequestByPatient(
                tenant,
                "fakeFaKEfAKefakE",
            )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, validResponse)
    }

    @Test
    fun `getMedicationRequestByPatient succeeds with date`() {
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
                "/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20,
                    "-timing-boundsPeriod" to "ge2023-09-06T00:00:00Z",
                ),
            )
        } returns ehrResponse

        val validResponse =
            medicationRequestService.getMedicationRequestByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                LocalDate.of(2023, 9, 6),
            )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, validResponse)
    }
}
