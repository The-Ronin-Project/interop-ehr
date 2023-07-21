package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.http.exceptions.ServerFailureException
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Practitioner
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

class EpicPractitionerServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var practitionerService: EpicPractitionerService
    private lateinit var practitionerRoleService: EpicPractitionerRoleService
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private lateinit var tenant: Tenant
    private val validPractitionerSearchBundle = readResource<Bundle>("/ExampleFindPractitionersResponse.json")
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
    fun `getPractitioner works when practitioner exists`() {
        val tenant = mockk<Tenant>()
        val mockPractitioner = mockk<Practitioner>(relaxed = true)

        coEvery {
            httpResponse.body<Practitioner>(
                TypeInfo(
                    Practitioner::class,
                    Practitioner::class.java
                )
            )
        } returns mockPractitioner
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/Practitioner/PracFHIRID") } returns ehrResponse

        val actual = practitionerService.getPractitioner(tenant, "PracFHIRID")
        assertEquals(mockPractitioner, actual)
    }

    @Test
    fun `getPractitioner propogates exceptions`() {
        val tenant = mockk<Tenant>()

        val thrownException = ClientFailureException(HttpStatusCode.NotFound, "Not Found")
        coEvery {
            httpResponse.body<Practitioner>(
                TypeInfo(
                    Practitioner::class,
                    Practitioner::class.java
                )
            )
        } throws thrownException
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/Practitioner/PracFHIRID") } returns ehrResponse

        val exception =
            assertThrows<ClientFailureException> { practitionerService.getPractitioner(tenant, "PracFHIRID") }

        assertEquals(thrownException, exception)
    }

    @Test
    fun `getPractitionerByProvider works when single practitioner found`() {
        val tenant = mockk<Tenant>()
        val mockPractitioner = mockk<Practitioner>(relaxed = true)

        coEvery { httpResponse.body<Bundle>() } returns mockBundle(mockPractitioner)
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Practitioner",
                mapOf("identifier" to "External|ProviderId", "_count" to 50)
            )
        } returns ehrResponse

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
                mapOf("identifier" to "External|ProviderId", "_count" to 50)
            )
        } returns ehrResponse

        assertThrows<NoSuchElementException> { practitionerService.getPractitionerByProvider(tenant, "ProviderId") }
    }

    @Test
    fun `getPractitionerByProvider throws exception when multiple practitioners found`() {
        val tenant = mockk<Tenant>()
        val mockPractitioner1 = mockk<Practitioner>(relaxed = true)
        val mockPractitioner2 = mockk<Practitioner>(relaxed = true)
        val mockPractitioner3 = mockk<Practitioner>(relaxed = true)

        coEvery { httpResponse.body<Bundle>() } returns mockBundle(
            mockPractitioner1,
            mockPractitioner2,
            mockPractitioner3
        )
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Practitioner",
                mapOf("identifier" to "External|ProviderId", "_count" to 50)
            )
        } returns ehrResponse

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
                mapOf("identifier" to "External|ProviderId", "_count" to 50)
            )
        } returns ehrResponse

        val exception =
            assertThrows<ServerFailureException> { practitionerService.getPractitionerByProvider(tenant, "ProviderId") }

        assertEquals(thrownException, exception)
    }

    @Test
    fun `getResourceListFromSearch responds`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            "testPrivateKey",
            "TEST_TENANT"
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
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Practitioner",
                mapOf(
                    "_id" to "1,2,3",
                    "_count" to 50
                )
            )
        } returns EHRResponse(httpResponse, "/Practitioner?_id=1,2,3")
        val parameters = mapOf(
            "_id" to "1,2,3",
            "_count" to 50
        )
        val bundle = practitionerService.getResourceListFromSearch(tenant, parameters)
        assertEquals(3, bundle.size)
    }

    @Test
    fun `getByIDs batches requests by batchSize`() {
        // show that a list of 5 IDs is chunked by batchPractitioners value 3
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            "testPrivateKey",
            "TEST_TENANT"
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
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Practitioner",
                mapOf(
                    "_id" to "1,2,3",
                    "_count" to 50
                )
            )
        } returns EHRResponse(httpResponse, "/Practitioner?_id=1,2,3")
        val httpResponse2: HttpResponse = mockk()
        coEvery { httpResponse2.body<Bundle>() } returns validPractitionerSearchBundle
        every { httpResponse2.status } returns HttpStatusCode.OK
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Practitioner",
                mapOf(
                    "_id" to "4,5",
                    "_count" to 50
                )
            )
        } returns EHRResponse(httpResponse2, "/Practitioner?_id=4,5")

        // batchPractitioners is 3 which chunks the 5 IDs:
        // IDs 1,2,3 return 3 mocked Practitioners, each having an Id value
        // IDs 4,5 returns our test resource JSON Bundle with 71 Practitioners
        val map = practitionerService.getByIDs(
            tenant,
            listOf("1", "2", "3", "4", "5")
        )
        assertEquals(74, map.size)
        assertTrue("1" in map.keys)
        assertTrue("2" in map.keys)
        assertTrue("3" in map.keys)
        assertTrue("e.tz0cM0q4ZzQ2lLNRueTqg3" in map.keys)
        assertTrue("e2gxs4Fn7l-TbzLO17uxuHw3" in map.keys)
        assertTrue("eFtrzfZw7Ma4DAGr8wngH0A3" in map.keys)
        val practitionerList = validPractitionerSearchBundle.entry.filter {
            it.resource?.resourceType == "Practitioner"
        }
        assertTrue(practitionerList.first().resource in map.values)
        assertTrue(practitionerList.last().resource in map.values)
    }

    @Test
    fun `getByIDs propagates exceptions`() {
        val thrownException = ClientFailureException(HttpStatusCode.NotFound, "Not Found")
        coEvery { httpResponse.body<Bundle>() } throws thrownException
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Practitioner",
                mapOf(
                    "_id" to "1,2",
                    "_count" to 50
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

    private fun <R : Resource<R>> mockBundle(vararg resources: R): Bundle {
        val entries = resources.map {
            mockk<BundleEntry> {
                every { resource } returns it
            }
        }

        return mockk(relaxed = true) {
            every { entry } returns entries
            every { link } returns emptyList()
        }
    }
}
