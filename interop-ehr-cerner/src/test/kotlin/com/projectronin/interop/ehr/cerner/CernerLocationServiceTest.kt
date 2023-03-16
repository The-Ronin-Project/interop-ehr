package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Location
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

class CernerLocationServiceTest {
    private lateinit var cernerClient: CernerClient
    private lateinit var httpResponse: HttpResponse
    private lateinit var locationService: CernerLocationService
    private val locationBundle = readResource<Bundle>("/ExampleLocationBundle.json")
    private val testTenant = createTestTenant(
        clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
        authEndpoint = "https://example.org",
        secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
        mrnSystem = "mrn"
    )

    @BeforeEach
    fun setup() {
        cernerClient = mockk()
        httpResponse = mockk()
        locationService = CernerLocationService(cernerClient, 5)
    }

    @Test
    fun `getLocationsByFHIRId - works`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns locationBundle
        coEvery {
            cernerClient.get(
                testTenant,
                "/Location",
                mapOf("_id" to "123,456", "_count" to 20)
            )
        } returns httpResponse

        val result = locationService.getLocationsByFHIRId(testTenant, listOf("123,456"))
        assertEquals(2, result.size)
    }

    @Test
    fun `getLocationsByFHIRId - chunked works`() {
        val smallLocationService = CernerLocationService(cernerClient, 2)
        val tenant = mockk<Tenant>()

        val location1 = mockk<BundleEntry> {
            every { resource } returns mockk<Location> {
                every { id!!.value } returns "123"
            }
        }
        val location2 = mockk<BundleEntry> {
            every { resource } returns mockk<Location> {
                every { id!!.value } returns "456"
            }
        }

        val location3 = mockk<BundleEntry> {
            every { resource } returns mockk<Location> {
                every { id!!.value } returns "789"
            }
        }
        val bundle = mockk<Bundle> {
            every { entry } returns listOf(location1, location2)
            every { link } returns emptyList()
        }
        val bundle2 = mockk<Bundle> {
            every { entry } returns listOf(location3)
            every { link } returns emptyList()
        }

        coEvery {
            cernerClient.get(tenant, "/Location", mapOf("_id" to "123,456", "_count" to 20))
                .body<Bundle>()
        } returns bundle
        coEvery {
            cernerClient.get(tenant, "/Location", mapOf("_id" to "789", "_count" to 20))
                .body<Bundle>()
        } returns bundle2

        val response =
            smallLocationService.getLocationsByFHIRId(tenant, listOf("123", "456", "789"))

        assertEquals(
            mapOf("123" to location1.resource, "456" to location2.resource, "789" to location3.resource),
            response
        )
    }
}
