package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
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

/**
 * Test findPractitionersByLocation() for both EpicPractitionerRoleService and
 * EpicPractitionerService, because the Epic API endpoint used in these tests is
 * for PractitionerRole. The function in EpicPractitionerService simply wraps a
 * call to findPractitionersByLocation() in EpicPractitionerRoleService.
 */
class EpicPractitionerRoleServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var practitionerService: EpicPractitionerService
    private lateinit var practitionerRoleService: EpicPractitionerRoleService
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private lateinit var tenant: Tenant
    private val validPractitionerSearchBundle = readResource<Bundle>("/ExampleFindPractitionersResponse.json")
    private val pagingPractitionerSearchBundle =
        readResource<Bundle>("/ExampleFindPractitionersResponseWithPaging.json")
    private val batchRoles: Int = 1
    private val batchPractitioners: Int = 3

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
        pagingHttpResponse = mockk()
        practitionerRoleService = EpicPractitionerRoleService(epicClient, batchRoles)
        practitionerService = EpicPractitionerService(epicClient, practitionerRoleService, batchPractitioners)
        tenant = mockk()
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
        coEvery { httpResponse.body<Bundle>() } returns validPractitionerSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to RepeatingParameter(
                        listOf(
                            "PractitionerRole:practitioner",
                            "PractitionerRole:location"
                        )
                    ),
                    "location" to "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03",
                    "_count" to 50
                )
            )
        } returns ehrResponse

        val bundle =
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03")
            )
        val expected = FindPractitionersResponse(validPractitionerSearchBundle)
        assertEquals(expected.practitionerRoles, bundle.practitionerRoles)
        assertEquals(expected.practitioners, bundle.practitioners)
        assertEquals(expected.locations, bundle.locations)
        assertEquals(expected.resources, bundle.resources)
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
        coEvery { httpResponse.body<Bundle>() } returns validPractitionerSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to RepeatingParameter(
                        listOf(
                            "PractitionerRole:practitioner",
                            "PractitionerRole:location"
                        )
                    ),
                    "location" to "abc",
                    "_count" to 50
                )
            )
        } returns ehrResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to RepeatingParameter(
                        listOf(
                            "PractitionerRole:practitioner",
                            "PractitionerRole:location"
                        )
                    ),
                    "location" to "123",
                    "_count" to 50
                )
            )
        } returns ehrResponse

        val bundle =
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("abc", "123")
            )

        // 142 = 71 practitioner roles from each of 2 locations, remove duplicates = 71
        assertEquals(71, bundle.practitionerRoles.size)
        // 142 becomes 71 total because duplicate practitioners are removed by EpicPractitionerBundle
        assertEquals(71, bundle.practitioners.size)
        // 106 becomes 53 total because duplicate practitioner locations are removed by EpicLocationBundle
        assertEquals(53, bundle.locations.size)
    }

    @Test
    fun `ensure multiple locations are supported with batching`() {
        val batchingPractitionerService = EpicPractitionerService(epicClient, practitionerRoleService, 2)

        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPractitionerSearchBundle

        /*
        Uncomment when we are no longer forcing batchSize to 1 in EpicPractitionerRoleService and remove the two cases below.
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to "PractitionerRole:practitioner,PractitionerRole:location", "location" to "loc1,loc2"
                )
            )
        } returns ehrResponse
         */
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to RepeatingParameter(
                        listOf(
                            "PractitionerRole:practitioner",
                            "PractitionerRole:location"
                        )
                    ),
                    "location" to "loc1",
                    "_count" to 50
                )
            )
        } returns ehrResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to RepeatingParameter(
                        listOf(
                            "PractitionerRole:practitioner",
                            "PractitionerRole:location"
                        )
                    ),
                    "location" to "loc2",
                    "_count" to 50
                )
            )
        } returns ehrResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to RepeatingParameter(
                        listOf(
                            "PractitionerRole:practitioner",
                            "PractitionerRole:location"
                        )
                    ),
                    "location" to "loc3",
                    "_count" to 50
                )
            )
        } returns ehrResponse

        val bundle =
            batchingPractitionerService.findPractitionersByLocation(
                tenant,
                listOf("loc1", "loc2", "loc3")
            )

        // 142 = 71 practitioner roles from each of 3 batch calls, remove duplicates = 71
        assertEquals(71, bundle.practitionerRoles.size)
        // 142 becomes 71 total because duplicate practitioners are removed by EpicPractitionerBundle
        assertEquals(71, bundle.practitioners.size)
        // 106 becomes 53 total because duplicate practitioner locations are removed by EpicLocationBundle
        assertEquals(53, bundle.locations.size)
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
        coEvery { pagingHttpResponse.body<Bundle>() } returns pagingPractitionerSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to RepeatingParameter(
                        listOf(
                            "PractitionerRole:practitioner",
                            "PractitionerRole:location"
                        )
                    ),
                    "location" to "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03",
                    "_count" to 50
                )
            )
        } returns EHRResponse(pagingHttpResponse, "67890")

        // Mock response without paging
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPractitionerSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "https://apporchard.epic.com/interconnect-aocurprd-oauth/api/FHIR/R4/PractitionerRole?_include=PractitionerRole:practitioner,PractitionerRole:location&location=e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03&sessionID=10-57E8BB9A4D4211EC94270050568B7BE6"
            )
        } returns ehrResponse

        val bundle =
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03")
            )

        // 2 Resources from the first query, 71 from the second, remove duplicates = 71
        assertEquals(71, bundle.practitionerRoles.size)

        // 2 of the practitioners are duplicates and get filtered out
        assertEquals(71, bundle.practitioners.size)

        // 2 of the practitioner locations are duplicates and get filtered out
        assertEquals(53, bundle.locations.size)
    }
}
