package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
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

internal class EpicOrganizationServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var organizationService: EpicOrganizationService
    private lateinit var tenant: Tenant
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private val validOrganizationResponse = readResource<Bundle>("/ExampleOrganizationBundle.json")

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
        organizationService = EpicOrganizationService(epicClient, 5)
        tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )
    }

    @Test
    fun `ensure organizations are returned with getByIDs`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validOrganizationResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Organization",
                mapOf(
                    "_id" to listOf("e8wMbBzuMGvZrYASWBHiL8w3"),
                ),
            )
        } returns ehrResponse

        val bundle =
            organizationService.getByIDs(
                tenant,
                listOf("e8wMbBzuMGvZrYASWBHiL8w3"),
            ).values.toList()

        val expectedOrganizationBundle = (validOrganizationResponse).entry.map { it.resource }
        assertEquals(expectedOrganizationBundle, bundle)
    }

    @Test
    fun `ensure organizations are returned`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validOrganizationResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Organization",
                mapOf(
                    "_id" to listOf("e8wMbBzuMGvZrYASWBHiL8w3"),
                ),
            )
        } returns ehrResponse

        val bundle =
            organizationService.findOrganizationsByFHIRId(
                tenant,
                listOf("e8wMbBzuMGvZrYASWBHiL8w3"),
            )

        val expectedOrganizationBundle = (validOrganizationResponse).entry.map { it.resource }
        assertEquals(expectedOrganizationBundle, bundle)
    }
}
