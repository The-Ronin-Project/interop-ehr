package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.http.exceptions.ServerFailureException
import com.projectronin.interop.ehr.cerner.client.CernerClient
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

class CernerPractitionerServiceTest {
    private lateinit var cernerClient: CernerClient
    private lateinit var practitionerService: CernerPractitionerService
    private lateinit var httpResponse: HttpResponse
    private lateinit var pagingHttpResponse: HttpResponse

    private val validPractitionerPaging = readResource<Bundle>("/ExamplePractitionerBundlePaging.json")

    @BeforeEach
    fun setup() {
        cernerClient = mockk()
        httpResponse = mockk()
        pagingHttpResponse = mockk()
        practitionerService = CernerPractitionerService(cernerClient)
    }

    @Test
    fun `getPractitioner works when practitioner exists`() {
        val tenant = mockk<Tenant>()
        val mockPractitioner = mockk<Practitioner>()

        coEvery { httpResponse.body<Practitioner>(TypeInfo(Practitioner::class, Practitioner::class.java)) } returns mockPractitioner
        coEvery { cernerClient.get(tenant, "/Practitioner/PractitionerFHIRID") } returns httpResponse

        val actual = practitionerService.getPractitioner(tenant, "PractitionerFHIRID")
        assertEquals(mockPractitioner, actual)
    }

    @Test
    fun `getPractitioner propagates exceptions`() {
        val tenant = mockk<Tenant>()

        val thrownException = ClientFailureException(HttpStatusCode.NotFound, "Not Found")
        coEvery { httpResponse.body<Practitioner>(TypeInfo(Practitioner::class, Practitioner::class.java)) } throws thrownException
        coEvery { cernerClient.get(tenant, "/Practitioner/PractitionerFHIRID") } returns httpResponse

        val exception =
            assertThrows<ClientFailureException> {
                practitionerService.getPractitioner(tenant, "PractitionerFHIRID")
            }

        assertEquals(thrownException, exception)
    }

    @Test
    fun `getPractitionerByProvider works when single practitioner found`() {
        val tenant = mockk<Tenant>()
        val mockPractitioner = mockk<Practitioner>()

        coEvery { httpResponse.body<Bundle>() } returns mockBundle(mockPractitioner)
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf("_id" to "ProviderId", "_count" to 20)
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
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf("_id" to "ProviderId", "_count" to 20)
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
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf("_id" to "ProviderId", "_count" to 20)
            )
        } returns httpResponse

        assertThrows<IllegalArgumentException> { practitionerService.getPractitionerByProvider(tenant, "ProviderId") }
    }

    @Test
    fun `getPractitionerByProvider propagates exceptions`() {
        val tenant = mockk<Tenant>()

        val thrownException = ServerFailureException(HttpStatusCode.InternalServerError, "Server Error")
        coEvery { httpResponse.body<Bundle>() } throws thrownException
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf("_id" to "ProviderId", "_count" to 20)
            )
        } returns httpResponse

        val exception =
            assertThrows<ServerFailureException> { practitionerService.getPractitionerByProvider(tenant, "ProviderId") }

        assertEquals(thrownException, exception)
    }

    @Test
    fun `findPractitionerByLocation throws exceptions`() {
        val tenant = mockk<Tenant>()

        val thrownException = NotImplementedError("Cerner does not support the PractitionerRole resource that connects Practitioners with Locations")
        coEvery { httpResponse.body<Bundle>() } throws thrownException
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf("_count" to 20)
            )
        } returns httpResponse

        val exception =
            assertThrows<NotImplementedError> {
                practitionerService.findPractitionersByLocation(
                    tenant, listOf("Fake")
                )
            }

        assertEquals(thrownException.message, exception.message)
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
        coEvery { pagingHttpResponse.body<Bundle>() } returns validPractitionerPaging
        coEvery {
            cernerClient.get(
                tenant,
                "/Practitioner",
                mapOf(
                    "_count" to 20
                )
            )
        } returns pagingHttpResponse

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
            mockk<BundleEntry> {
                every { resource } returns it
            }
        }

        return mockk {
            every { entry } returns entries
            every { link } returns emptyList()
        }
    }
}
