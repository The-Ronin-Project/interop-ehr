package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.http.exceptions.ServerFailureException
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
import com.projectronin.interop.fhir.r4.datatype.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.resource.Resource
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
        practitionerService = EpicPractitionerService(epicClient, 1)
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
                    "_include" to listOf("PractitionerRole:practitioner", "PractitionerRole:location"),
                    "location" to "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03",
                    "_count" to 50
                )
            )
        } returns httpResponse

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
                    "_include" to listOf("PractitionerRole:practitioner", "PractitionerRole:location"),
                    "location" to "abc",
                    "_count" to 50
                )
            )
        } returns httpResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to listOf("PractitionerRole:practitioner", "PractitionerRole:location"),
                    "location" to "123",
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("abc", "123")
            )

        // 142 = 71 practitioner roles from each of 2 locations
        assertEquals(142, bundle.practitionerRoles.size)
        // 142 becomes 71 total because duplicate practitioners are removed by EpicPractitionerBundle
        assertEquals(71, bundle.practitioners.size)
        // 106 becomes 53 total because duplicate practitioner locations are removed by EpicLocationBundle
        assertEquals(53, bundle.locations.size)
    }

    @Test
    fun `ensure multiple locations are supported with batching`() {
        val batchingPractitionerService = EpicPractitionerService(epicClient, 2)

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
        Uncomment when we are no longer forcing batchSize to 1 in EpicPractitioner and remove the two cases below.
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to "PractitionerRole:practitioner,PractitionerRole:location", "location" to "loc1,loc2"
                )
            )
        } returns httpResponse
         */
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to listOf("PractitionerRole:practitioner", "PractitionerRole:location"),
                    "location" to "loc1",
                    "_count" to 50
                )
            )
        } returns httpResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to listOf("PractitionerRole:practitioner", "PractitionerRole:location"),
                    "location" to "loc2",
                    "_count" to 50
                )
            )
        } returns httpResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to listOf("PractitionerRole:practitioner", "PractitionerRole:location"),
                    "location" to "loc3",
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            batchingPractitionerService.findPractitionersByLocation(
                tenant,
                listOf("loc1", "loc2", "loc3")
            )

        // 142 = 71 practitioner roles from each of 3 batch calls
        assertEquals(213, bundle.practitionerRoles.size)
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
                    "_include" to listOf("PractitionerRole:practitioner", "PractitionerRole:location"),
                    "location" to "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03",
                    "_count" to 50
                )
            )
        } returns pagingHttpResponse

        // Mock response without paging
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPractitionerSearchBundle
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
        assertEquals(73, bundle.practitionerRoles.size)

        // 2 of the practitioners are duplicates and get filtered out
        assertEquals(71, bundle.practitioners.size)

        // 2 of the practitioner locations are duplicates and get filtered out
        assertEquals(53, bundle.locations.size)
    }

    @Test
    fun `getPractitioner works when practitioner exists`() {
        val tenant = mockk<Tenant>()
        val mockPractitioner = mockk<Practitioner>()

        coEvery { httpResponse.body<Practitioner>() } returns mockPractitioner
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/Practitioner/PracFHIRID") } returns httpResponse

        val actual = practitionerService.getPractitioner(tenant, "PracFHIRID")
        assertEquals(mockPractitioner, actual)
    }

    @Test
    fun `getPractitioner propogates exceptions`() {
        val tenant = mockk<Tenant>()

        val thrownException = ClientFailureException(HttpStatusCode.NotFound, "Not Found")
        coEvery { httpResponse.body<Practitioner>() } throws thrownException
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/Practitioner/PracFHIRID") } returns httpResponse

        val exception =
            assertThrows<ClientFailureException> { practitionerService.getPractitioner(tenant, "PracFHIRID") }

        assertEquals(thrownException, exception)
    }

    @Test
    fun `getPractitionerByProvider works when single practitioner found`() {
        val tenant = mockk<Tenant>()
        val mockPractitioner = mockk<Practitioner>()

        coEvery { httpResponse.body<Bundle>() } returns mockBundle(mockPractitioner)
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Practitioner",
                mapOf("identifier" to "External|ProviderId")
            )
        } returns httpResponse

        val actual = practitionerService.getPractitionerByProvider(tenant, "ProviderId")
        assertEquals(mockPractitioner, actual)
    }

    @Test
    fun `getPractitionerByProvider throws exception when no practitioner found`() {
        val tenant = mockk<Tenant>()

        coEvery { httpResponse.body<Bundle>() } returns mockBundle()
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Practitioner",
                mapOf("identifier" to "External|ProviderId")
            )
        } returns httpResponse

        assertThrows<NoSuchElementException> { practitionerService.getPractitionerByProvider(tenant, "ProviderId") }
    }

    @Test
    fun `getPractitionerByProvider throws exception when multiple practitioners found`() {
        val tenant = mockk<Tenant>()
        val mockPractitioner1 = mockk<Practitioner>()
        val mockPractitioner2 = mockk<Practitioner>()
        val mockPractitioner3 = mockk<Practitioner>()

        coEvery { httpResponse.body<Bundle>() } returns mockBundle(
            mockPractitioner1,
            mockPractitioner2,
            mockPractitioner3
        )
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Practitioner",
                mapOf("identifier" to "External|ProviderId")
            )
        } returns httpResponse

        assertThrows<IllegalArgumentException> { practitionerService.getPractitionerByProvider(tenant, "ProviderId") }
    }

    @Test
    fun `getPractitionerByProvider propogates exceptions`() {
        val tenant = mockk<Tenant>()

        val thrownException = ServerFailureException(HttpStatusCode.InternalServerError, "Server Error")
        coEvery { httpResponse.body<Bundle>() } throws thrownException
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Practitioner",
                mapOf("identifier" to "External|ProviderId")
            )
        } returns httpResponse

        val exception =
            assertThrows<ServerFailureException> { practitionerService.getPractitionerByProvider(tenant, "ProviderId") }

        assertEquals(thrownException, exception)
    }

    private fun <R : Resource<R>> mockBundle(vararg resources: R): Bundle {
        val entries = resources.map {
            mockk<BundleEntry> {
                every { resource } returns it
            }
        }

        return mockk {
            every { entry } returns entries
        }
    }
}
