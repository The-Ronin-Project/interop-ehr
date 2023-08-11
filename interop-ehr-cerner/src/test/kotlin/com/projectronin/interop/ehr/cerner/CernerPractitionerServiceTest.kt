package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.http.exceptions.ServerFailureException
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.util.reflect.TypeInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CernerPractitionerServiceTest {
    private lateinit var cernerClient: CernerClient
    private lateinit var practitionerService: CernerPractitionerService
    private lateinit var practitionerRoleService: CernerPractitionerRoleService
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private lateinit var tenant: Tenant
    private val validPractitionerBundle = readResource<Bundle>("/ExamplePractitionerBundle.json")
    private val validPractitionerPagingBundle = readResource<Bundle>("/ExamplePractitionerBundlePaging.json")
    private val batchPractitioners: Int = 3

    @BeforeEach
    fun setup() {
        cernerClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
        pagingHttpResponse = mockk()
        tenant = mockk()
        practitionerRoleService = CernerPractitionerRoleService(cernerClient)
        practitionerService = CernerPractitionerService(cernerClient, practitionerRoleService, batchPractitioners)
    }

    @Test
    fun `getPractitioner works when practitioner exists`() {
        val mockPractitioner = mockk<Practitioner>(relaxed = true)

        coEvery {
            httpResponse.body<Practitioner>(
                TypeInfo(
                    Practitioner::class,
                    Practitioner::class.java
                )
            )
        } returns mockPractitioner
        coEvery { cernerClient.get(tenant, "/Practitioner/PractitionerFHIRID") } returns ehrResponse

        val actual = practitionerService.getPractitioner(tenant, "PractitionerFHIRID")
        assertEquals(mockPractitioner, actual)
    }

    @Test
    fun `getPractitioner propagates exceptions`() {
        val thrownException = ClientFailureException(HttpStatusCode.NotFound, "Not Found")
        coEvery {
            httpResponse.body<Practitioner>(
                TypeInfo(
                    Practitioner::class,
                    Practitioner::class.java
                )
            )
        } throws thrownException
        coEvery { cernerClient.get(tenant, "/Practitioner/PractitionerFHIRID") } returns ehrResponse

        val exception =
            assertThrows<ClientFailureException> {
                practitionerService.getPractitioner(tenant, "PractitionerFHIRID")
            }

        assertEquals(thrownException, exception)
    }

    @Test
    fun `getPractitionerByProvider works when single practitioner found`() {
        val mockPractitioner = mockk<Practitioner>(relaxed = true)

        coEvery { httpResponse.body<Bundle>() } returns mockBundle(mockPractitioner)
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf("_id" to "ProviderId")
            )
        } returns ehrResponse

        val actual = practitionerService.getPractitionerByProvider(tenant, "ProviderId")
        assertEquals(mockPractitioner, actual)
    }

    @Test
    fun `getPractitionerByProvider throws exception when no practitioner found`() {
        coEvery { httpResponse.body<Bundle>() } returns mockBundle()
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf("_id" to "ProviderId")
            )
        } returns ehrResponse

        assertThrows<NoSuchElementException> { practitionerService.getPractitionerByProvider(tenant, "ProviderId") }
    }

    @Test
    fun `getPractitionerByProvider throws exception when multiple practitioners found`() {
        val mockPractitioner1 = mockk<Practitioner>(relaxed = true)
        val mockPractitioner2 = mockk<Practitioner>(relaxed = true)
        val mockPractitioner3 = mockk<Practitioner>(relaxed = true)

        coEvery { httpResponse.body<Bundle>() } returns mockBundle(
            mockPractitioner1,
            mockPractitioner2,
            mockPractitioner3
        )
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf("_id" to "ProviderId")
            )
        } returns ehrResponse

        assertThrows<IllegalArgumentException> { practitionerService.getPractitionerByProvider(tenant, "ProviderId") }
    }

    @Test
    fun `getPractitionerByProvider propagates exceptions`() {
        val thrownException = ServerFailureException(HttpStatusCode.InternalServerError, "Server Error")
        coEvery { httpResponse.body<Bundle>() } throws thrownException
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf("_id" to "ProviderId")
            )
        } returns ehrResponse

        val exception =
            assertThrows<ServerFailureException> { practitionerService.getPractitionerByProvider(tenant, "ProviderId") }

        assertEquals(thrownException, exception)
    }

    @Test
    fun `findPractitionerByLocation returns empty result`() {
        val actualResult = practitionerRoleService.findPractitionersByLocation(tenant, listOf("123", "321"))

        var expectedResult = FindPractitionersResponse(
            Bundle(
                type = null
            )
        )

        assertEquals(expectedResult.resource, actualResult.resource)
        assertEquals(emptyList<Resource<*>>(), actualResult.resources)
        assertEquals(emptyList<PractitionerRole>(), actualResult.practitionerRoles)
        assertEquals(emptyList<Practitioner>(), actualResult.practitioners)
        assertEquals(emptyList<Location>(), actualResult.locations)
    }

    @Test
    fun `ensure paging works`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        // Mock response with paging
        every { pagingHttpResponse.status } returns HttpStatusCode.OK
        coEvery { pagingHttpResponse.body<Bundle>() } returns validPractitionerPagingBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf(
                    "_count" to 20
                )
            )
        } returns EHRResponse(pagingHttpResponse, "67890")

        val parameters = mapOf(
            "_count" to 20
        )
        val bundle = practitionerService.getResourceListFromSearch(tenant, parameters)

        assertEquals(10, bundle.size)
        assertTrue(
            bundle.any {
                it.resourceType == "Practitioner"
            }
        )
        assertTrue(
            bundle.any {
                it.active?.value is Boolean
            }
        )
        assertTrue(
            bundle.any {
                it.id?.value!!.isNotEmpty()
            }
        )
        assertTrue(
            bundle.any {
                it.name.isNotEmpty()
            }
        )
    }

    private fun <R : Resource<R>> mockBundle(vararg resources: R): Bundle {
        val entries = resources.map {
            mockk<BundleEntry>(relaxed = true) {
                every { resource } returns it
            }
        }

        return mockk(relaxed = true) {
            every { entry } returns entries
            every { link } returns emptyList()
        }
    }

    @Test
    fun `getResourceListFromSearch responds`() {
        val tenant = createTestTenant(
            clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
            authEndpoint = "https://example.org",
            secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
        )
        val mockPractitioner1 = mockk<Practitioner>(relaxed = true)
        val mockPractitioner2 = mockk<Practitioner>(relaxed = true)
        val mockPractitioner3 = mockk<Practitioner>(relaxed = true)
        coEvery { httpResponse.body<Bundle>() } returns mockBundle(
            mockPractitioner1,
            mockPractitioner2,
            mockPractitioner3
        )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf(
                    "_id" to "1,2,3"
                )
            )
        } returns EHRResponse(httpResponse, "/Practitioner?_id=1,2,3")
        val parameters = mapOf(
            "_id" to "1,2,3"
        )

        val bundle = practitionerService.getResourceListFromSearch(tenant, parameters)
        assertEquals(3, bundle.size)
    }

    @Test
    fun `getByIDs batches requests by practitionerBatch size`() {
        // show that a list of 5 IDs is chunked by batchPractitioners value 3
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        val mockPractitioner1 = mockk<Practitioner>(relaxed = true) {
            every { id } returns Id("1")
        }
        val mockPractitioner2 = mockk<Practitioner>(relaxed = true) {
            every { id } returns Id("2")
        }
        val mockPractitioner3 = mockk<Practitioner>(relaxed = true) {
            every { id } returns Id("3")
        }
        coEvery { httpResponse.body<Bundle>() } returns mockBundle(
            mockPractitioner1,
            mockPractitioner2,
            mockPractitioner3
        )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf(
                    "_id" to listOf("1", "2", "3")
                )
            )
        } returns EHRResponse(httpResponse, "/Practitioner?_id=1,2,3")

        val httpResponse2: HttpResponse = mockk()
        coEvery { httpResponse2.body<Bundle>() } returns validPractitionerBundle
        every { httpResponse2.status } returns HttpStatusCode.OK
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf(
                    "_id" to listOf("4", "5")
                )
            )
        } returns EHRResponse(httpResponse2, "/Practitioner?_id=4,5")

        // batchPractitioners is 3 which chunks the 5 IDs:
        // IDs 1,2,3 return 3 mocked Practitioners, each having an Id value
        // IDs 4,5 returns our test resource JSON Bundle with 1 Practitioner
        val map = practitionerService.getByIDs(
            tenant,
            listOf("1", "2", "3", "4", "5")
        )
        assertEquals(4, map.size)
        assertTrue("1" in map.keys)
        assertTrue("2" in map.keys)
        assertTrue("3" in map.keys)
        assertTrue("12793559" in map.keys)
        assertTrue(validPractitionerBundle.entry.first().resource in map.values)
    }

    @Test
    fun `getByIDs propagates exceptions`() {
        val thrownException = ClientFailureException(HttpStatusCode.NotFound, "Not Found")
        coEvery { httpResponse.body<Bundle>() } throws thrownException
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf(
                    "_id" to listOf("1", "2")
                )
            )
        } returns ehrResponse

        val exception = assertThrows<ClientFailureException> {
            practitionerService.getByIDs(
                tenant,
                listOf("1", "2")
            )
        }
        assertEquals(thrownException, exception)
    }
}
