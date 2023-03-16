package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.MedicationRequestService
import com.projectronin.interop.ehr.cerner.client.CernerClient
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

    private val validPatientIdBundle = readResource<Bundle>("/ExampleMedicationRequestPatientIdBundle.json")
    private val validMedIdReturn = readResource<MedicationRequest>("/ExampleMedicalRequestByIdResponse.json")

    @Test
    fun `getMedicationRequestById works for medication Id`() {
        val tenant = mockk<Tenant>()
        val mockMedicationRequest = mockk<MedicationRequest>()

        coEvery { httpResponse.body<MedicationRequest>(TypeInfo(MedicationRequest::class, MedicationRequest::class.java)) } returns mockMedicationRequest
        coEvery { cernerClient.get(tenant, "/MedicationRequest/fakeFaKEfAKefakE") } returns httpResponse

        val actual = medicationRequestService.getMedicationRequestById(tenant, "fakeFaKEfAKefakE")
        assertEquals(mockMedicationRequest, actual)
    }

    @Test
    fun `getMedicationRequestById with medication request id returns medication request resource`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )
        val service = mockk<MedicationRequestService>()
        every {
            service.getMedicationRequestById(
                tenant,
                "fakeFaKEfAKefakE"
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
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientIdBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20
                )
            )
        } returns httpResponse

        val actual = medicationRequestService.getMedicationRequestByPatient(
            tenant,
            "fakeFaKEfAKefakE",
            null,
            null
        )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, actual)
        assertEquals(25, actual.size)
        actual.forEachIndexed { _, medicationRequest ->
            val subject = medicationRequest.subject?.reference?.value
            assertTrue(subject == "Patient/fakeFaKEfAKefakE")
        }
    }

    @Test
    fun `getMedicationRequestByPatient with null dates returns patient medication request bundle`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientIdBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20
                )
            )
        } returns httpResponse

        val actual = medicationRequestService.getMedicationRequestByPatient(
            tenant,
            "fakeFaKEfAKefakE",
            null,
            null
        )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, actual)
        assertEquals(25, actual.size)
        actual.forEachIndexed { _, medicationRequest ->
            val subject = medicationRequest.subject?.reference?.value
            assertTrue(subject == "Patient/fakeFaKEfAKefakE")
        }
    }

    @Test
    fun `getMedicationRequestByPatient success when null startDate and actual endDate are provided`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientIdBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20
                )
            )
        } returns httpResponse

        val validResponse =
            medicationRequestService.getMedicationRequestByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                null,
                LocalDate.of(2019, 11, 1)
            )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, validResponse)
    }

    @Test
    fun `getMedicationRequestByPatient success when actual startDate and null endDate are provided`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientIdBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20
                )
            )
        } returns httpResponse

        val validResponse =
            medicationRequestService.getMedicationRequestByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                LocalDate.of(2018, 1, 1),
                null
            )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, validResponse)
    }

    @Test
    fun `getMedicationRequestByPatient success when both startDate and endDate are provided and ignored`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientIdBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 20
                )
            )
        } returns httpResponse

        val validResponse =
            medicationRequestService.getMedicationRequestByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2019, 11, 1)
            )

        assertEquals(validPatientIdBundle.entry.map { it.resource }, validResponse)
    }
}
