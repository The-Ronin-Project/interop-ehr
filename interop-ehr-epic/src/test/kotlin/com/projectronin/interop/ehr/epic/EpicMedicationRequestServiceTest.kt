package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.ehr.MedicationRequestService
import com.projectronin.interop.ehr.epic.client.EpicClient
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class EpicMedicationRequestServiceTest {
    private val epicClient: EpicClient = mockk()
    private val medicationRequestService: MedicationRequestService = EpicMedicationRequestService(epicClient)
    private val httpResponse: HttpResponse = mockk()

    private val medicationRequestReturnBundle = readResource<Bundle>("/ExampleMedicationRequestBundle.json")

    @Test
    fun `getMedicationRequestById - works with one medication`() {
        val tenant = mockk<Tenant>()
        val medicationRequest = mockk<MedicationRequest>()

        coEvery { httpResponse.body<MedicationRequest>(TypeInfo(MedicationRequest::class, MedicationRequest::class.java)) } returns medicationRequest
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/MedicationRequest/fakeFaKEfAKefakE") } returns httpResponse

        val actual = medicationRequestService.getMedicationRequestById(tenant, "fakeFaKEfAKefakE")
        assertEquals(medicationRequest, actual)
    }

    @Test
    fun `getMedicationRequestById throws exception`() {
        val tenant = mockk<Tenant>()
        val thrownException = ClientFailureException(HttpStatusCode.NotFound, "Not Found")
        coEvery { httpResponse.body<MedicationRequest>(TypeInfo(MedicationRequest::class, MedicationRequest::class.java)) } throws thrownException
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/MedicationRequest/fakeFaKEfAKefakE") } returns httpResponse

        val exception =
            assertThrows<ClientFailureException> { medicationRequestService.getMedicationRequestById(tenant, "fakeFaKEfAKefakE") }

        assertEquals(thrownException, exception)
    }

    @Test
    fun `getMedicationRequestByPatient returns patient medication request bundle`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns medicationRequestReturnBundle

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "date" to listOf("ge2018-01-01", "le2019-11-01"),
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            medicationRequestService.getMedicationRequestByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2019, 11, 1)
            )

        assertEquals(medicationRequestReturnBundle.entry.map { it.resource }, bundle)
    }

    @Test
    fun `getMedicationRequestByPatient returns patient medication request bundle with only endDate given`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns medicationRequestReturnBundle

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "date" to listOf("le2019-11-01"),
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            medicationRequestService.getMedicationRequestByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                null,
                LocalDate.of(2019, 11, 1)
            )

        assertEquals(medicationRequestReturnBundle.entry.map { it.resource }, bundle)
    }

    @Test
    fun `getMedicationRequestByPatient returns patient medication request bundle with only startDate given`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns medicationRequestReturnBundle

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "date" to listOf("ge2018-01-01"),
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            medicationRequestService.getMedicationRequestByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                LocalDate.of(2018, 1, 1),
                null
            )

        assertEquals(medicationRequestReturnBundle.entry.map { it.resource }, bundle)
    }

    @Test
    fun `getMedicationRequestByPatient success with null values for startDate and endDate`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns medicationRequestReturnBundle

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/MedicationRequest",
                mapOf(
                    "patient" to "fakeFaKEfAKefakE",
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            medicationRequestService.getMedicationRequestByPatient(
                tenant,
                "fakeFaKEfAKefakE",
                null,
                null
            )

        assertEquals(medicationRequestReturnBundle.entry.map { it.resource }, bundle)
    }
}
