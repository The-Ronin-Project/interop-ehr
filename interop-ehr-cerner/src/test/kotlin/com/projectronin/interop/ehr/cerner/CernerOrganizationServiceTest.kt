package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CernerOrganizationServiceTest {
    private lateinit var cernerClient: CernerClient
    private lateinit var organizationService: CernerOrganizationService
    private lateinit var tenant: Tenant
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private lateinit var httpPagingResponse: HttpResponse
    private lateinit var ehrPagingResponse: EHRResponse
    private val validOrganizationResponse = readResource<Bundle>("/ExampleOrganizationBundle.json")
    private val validOrganizationResponseWithPaging = readResource<Bundle>("/ExampleOrganizationBundleWithPaging.json")
    private val batchSize = 3

    @BeforeEach
    fun setup() {
        cernerClient = mockk()
        httpResponse = mockk()
        httpPagingResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
        ehrPagingResponse = EHRResponse(httpPagingResponse, "54321")
        organizationService = CernerOrganizationService(cernerClient, batchSize)
        tenant = createTestTenant()
    }

    @Test
    fun `ensure organizations are returned`() {
        val fhirId = "3170039"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validOrganizationResponse
        coEvery {
            cernerClient.get(
                tenant,
                "/Organization",
                mapOf(
                    "_id" to fhirId,
                    "_count" to 20
                )
            )
        } returns ehrResponse

        val bundle = organizationService.getByIDs(
            tenant,
            listOf(fhirId)
        ).values.toList()

        val expectedOrganizationBundle = validOrganizationResponse.entry.map { it.resource }
        assertEquals(expectedOrganizationBundle, bundle)
    }

    @Test
    fun `ensure organizations are returned when paging`() {
        val fhirIds = listOf("123456", "3170039")

        every { httpPagingResponse.status } returns HttpStatusCode.OK
        coEvery { httpPagingResponse.body<Bundle>() } returns validOrganizationResponseWithPaging
        coEvery {
            cernerClient.get(
                tenant,
                "/Organization",
                mapOf(
                    "_id" to fhirIds.joinToString(separator = ","),
                    "_count" to 20
                )
            )
        } returns ehrPagingResponse

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validOrganizationResponse
        coEvery {
            cernerClient.get(
                tenant,
                "https://fhir-ehr-code.cerner.com/r4/ec2458f2-1e24-41c8-b71b-0e701bf7583d/Organization?-offset=100"
            )
        } returns ehrResponse

        val bundle = organizationService.getByIDs(
            tenant,
            fhirIds
        ).values.toList()

        val expectedBundle = validOrganizationResponseWithPaging.entry.map { it.resource } +
            validOrganizationResponse.entry.map { it.resource }
        assertEquals(2, bundle.size)
        assertEquals(expectedBundle, bundle)
    }

    @Test
    fun `ensure organizations are returned by findOrganizationsByFHIRId`() {
        val fhirId = "3170039"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validOrganizationResponse
        coEvery {
            cernerClient.get(
                tenant,
                "/Organization",
                mapOf(
                    "_id" to fhirId,
                    "_count" to 20
                )
            )
        } returns ehrResponse

        val bundle = organizationService.findOrganizationsByFHIRId(
            tenant,
            listOf(fhirId)
        )

        val expectedOrganizationBundle = validOrganizationResponse.entry.map { it.resource }
        assertEquals(expectedOrganizationBundle, bundle)
    }

    @Test
    fun `ensure organizations are returned findOrganizationsByFHIRId when paging`() {
        val fhirIds = listOf("123456", "3170039")

        every { httpPagingResponse.status } returns HttpStatusCode.OK
        coEvery { httpPagingResponse.body<Bundle>() } returns validOrganizationResponseWithPaging
        coEvery {
            cernerClient.get(
                tenant,
                "/Organization",
                mapOf(
                    "_id" to fhirIds.joinToString(separator = ","),
                    "_count" to 20
                )
            )
        } returns ehrPagingResponse

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validOrganizationResponse
        coEvery {
            cernerClient.get(
                tenant,
                "https://fhir-ehr-code.cerner.com/r4/ec2458f2-1e24-41c8-b71b-0e701bf7583d/Organization?-offset=100"
            )
        } returns ehrResponse

        val bundle = organizationService.findOrganizationsByFHIRId(
            tenant,
            fhirIds
        )

        val expectedBundle = validOrganizationResponseWithPaging.entry.map { it.resource } +
            validOrganizationResponse.entry.map { it.resource }
        assertEquals(2, bundle.size)
        assertEquals(expectedBundle, bundle)
    }
}
