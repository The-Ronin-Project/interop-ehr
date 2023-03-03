package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.MedicationStatementService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CernerMedicationStatementServiceTest {
    private lateinit var tenant: Tenant
    private lateinit var cernerClient: CernerClient
    private lateinit var medicationStatementService: MedicationStatementService
    private lateinit var httpResponse: HttpResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private val validMedicationStatementResponse = readResource<Bundle>("/ExampleMedicationStatementBundle.json")
    private val pagingMedicationStatementResponse =
        readResource<Bundle>("/ExampleMedicationStatementBundleWithPaging.json")
    private val emptyMedicationStatementResponse = readResource<Bundle>("/DSTU2BundleEmpty.json")
    private val searchUrlPart = "/MedicationStatement"

    @BeforeEach
    fun setup() {
        cernerClient = mockk()
        httpResponse = mockk()
        pagingHttpResponse = mockk()
        medicationStatementService = CernerMedicationStatementService(cernerClient)
        tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )
    }

    @Test
    fun `ensure medicationstatements are returned`() {
        val patientFHIRId = "12724067"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validMedicationStatementResponse
        coEvery {
            cernerClient.get(
                tenant,
                searchUrlPart,
                mapOf(
                    "patient" to patientFHIRId,
                    "_count" to 20
                )
            )
        } returns httpResponse

        val bundle = medicationStatementService.getMedicationStatementsByPatientFHIRId(
            tenant,
            patientFHIRId
        )

        val expectedMedicationStatementBundle = validMedicationStatementResponse.entry.map { it.resource }

        assertEquals(expectedMedicationStatementBundle, bundle)
    }

    @Test
    fun `0 medication statements are returned`() {
        val patientFHIRID = "12345678"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns emptyMedicationStatementResponse
        coEvery {
            cernerClient.get(
                tenant,
                searchUrlPart,
                mapOf(
                    "patient" to patientFHIRID,
                    "_count" to 20
                )
            )
        } returns httpResponse

        val emptyBundle = medicationStatementService.getMedicationStatementsByPatientFHIRId(
            tenant,
            patientFHIRID
        )

        val expectedMedicationStatementBundle = emptyMedicationStatementResponse.entry.map { it.resource }
        assertEquals(expectedMedicationStatementBundle, emptyBundle)
    }

    @Test
    fun `ensure paging is called`() {
        val patientFHIRId = "12724067"
        val nextUrl = "https://fhir-open.cerner.com/dstu2/ec2458f2-1e24-41c8-b71b-0e701af7583d/MedicationStatement?NEXT"

        every { pagingHttpResponse.status } returns HttpStatusCode.OK
        coEvery { pagingHttpResponse.body<Bundle>() } returns pagingMedicationStatementResponse
        coEvery {
            cernerClient.get(
                tenant,
                searchUrlPart,
                mapOf(
                    "patient" to patientFHIRId,
                    "_count" to 20
                )
            )
        } returns pagingHttpResponse

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validMedicationStatementResponse
        coEvery {
            cernerClient.get(
                tenant,
                nextUrl
            )
        } returns httpResponse

        val bundleFromPaging = medicationStatementService.getMedicationStatementsByPatientFHIRId(
            tenant,
            patientFHIRId
        )

        coVerify {
            cernerClient.get(
                tenant,
                nextUrl
            )
        }
    }

    @Test
    fun `ensure paging returns correct number of resources`() {
        val patientFHIRId = "12724067"
        val nextUrl = "https://fhir-open.cerner.com/dstu2/ec2458f2-1e24-41c8-b71b-0e701af7583d/MedicationStatement?NEXT"

        every { pagingHttpResponse.status } returns HttpStatusCode.OK
        coEvery { pagingHttpResponse.body<Bundle>() } returns pagingMedicationStatementResponse
        coEvery {
            cernerClient.get(
                tenant,
                searchUrlPart,
                mapOf(
                    "patient" to patientFHIRId,
                    "_count" to 20
                )
            )
        } returns pagingHttpResponse

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validMedicationStatementResponse
        coEvery {
            cernerClient.get(
                tenant,
                nextUrl
            )
        } returns httpResponse

        val bundleFromPaging = medicationStatementService.getMedicationStatementsByPatientFHIRId(
            tenant,
            patientFHIRId
        )

        assertEquals(2, bundleFromPaging.size)
    }
}
