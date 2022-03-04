package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicFindPractitionersResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import io.ktor.client.call.receive
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicPractitionerServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var practitionerService: EpicPractitionerService
    private lateinit var httpResponse: HttpResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private val validPractitionerSearchBundle = readResource<Bundle>("/ExampleFindPractitionersResponse.json")
    private val pagingPractitionerSearchBundle =
        readResource<Bundle>("/ExampleFindPractitionersResponseWithPaging.json")

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
        pagingHttpResponse = mockk()
        practitionerService = EpicPractitionerService(epicClient)
    }

    @Test
    fun `ensure practitioners are returned`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<Bundle>() } returns validPractitionerSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to "PractitionerRole:practitioner,PractitionerRole:location",
                    "location" to "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03"
                )
            )
        } returns httpResponse

        val bundle =
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03")
            )

        assertEquals(EpicFindPractitionersResponse(validPractitionerSearchBundle), bundle)
        assertEquals(71, bundle.practitionerRoles!!.resources.size)
        assertEquals(71, bundle.practitioners?.resources?.size)
        assertEquals(53, bundle.locations?.resources?.size)
    }

    @Test
    fun `ensure http error handled`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.NotFound
        coEvery { httpResponse.receive<Bundle>() } returns validPractitionerSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to "PractitionerRole:practitioner,PractitionerRole:location",
                    "location" to "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03"
                )
            )
        } returns httpResponse

        assertThrows<IOException> {
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03")
            )
        }
    }

    @Test
    fun `ensure multiple locations are supported`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<Bundle>() } returns validPractitionerSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf("_include" to "PractitionerRole:practitioner,PractitionerRole:location", "location" to "abc")
            )
        } returns httpResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf("_include" to "PractitionerRole:practitioner,PractitionerRole:location", "location" to "123")
            )
        } returns httpResponse

        val bundle =
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("abc", "123")
            )

        // 142 = 71 practitioner roles from each of 2 locations
        assertEquals(142, bundle.practitionerRoles!!.resources.size)
        // 142 becomes 71 total because duplicate practitioners are removed by EpicPractitionerBundle
        assertEquals(71, bundle.practitioners?.resources?.size)
        // 106 becomes 53 total because duplicate practitioner locations are removed by EpicLocationBundle
        assertEquals(53, bundle.locations?.resources?.size)
    }

    @Test
    fun `ensure paging works`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        // Mock response with paging
        every { pagingHttpResponse.status } returns HttpStatusCode.OK
        coEvery { pagingHttpResponse.receive<Bundle>() } returns pagingPractitionerSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to "PractitionerRole:practitioner,PractitionerRole:location",
                    "location" to "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03"
                )
            )
        } returns pagingHttpResponse

        // Mock response without paging
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<Bundle>() } returns validPractitionerSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "https://apporchard.epic.com/interconnect-aocurprd-oauth/api/FHIR/R4/PractitionerRole?_include=PractitionerRole:practitioner,PractitionerRole:location&location=e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03&sessionID=10-57E8BB9A4D4211EC94270050568B7BE6"
            )
        } returns httpResponse

        val bundle =
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03")
            )

        // 2 Resources from the first query, 71 from the second
        assertEquals(73, bundle.practitionerRoles?.resources?.size)

        // 2 of the practitioners are duplicates and get filtered out
        assertEquals(71, bundle.practitioners?.resources?.size)

        // 2 of the practitioner locations are duplicates and get filtered out
        assertEquals(53, bundle.locations?.resources?.size)
    }
}
