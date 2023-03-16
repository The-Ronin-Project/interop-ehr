package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.LocationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EpicLocationServiceTest {
    private var epicClient: EpicClient = mockk()
    private var locationService: LocationService = EpicLocationService(epicClient, 5)

    @Test
    fun getLocationsByFHIRId() {
        val tenant = mockk<Tenant>()

        val location1 = mockk<BundleEntry> {
            every { resource } returns mockk<Location> {
                every { id!!.value } returns "12345"
            }
        }
        val location2 = mockk<BundleEntry> {
            every { resource } returns mockk<Location> {
                every { id!!.value } returns "67890"
            }
        }
        val bundle = mockk<Bundle> {
            every { entry } returns listOf(location1, location2)
            every { link } returns emptyList()
        }

        coEvery {
            epicClient.get(tenant, "/api/FHIR/R4/Location", mapOf("_id" to "12345,67890", "_count" to 50))
                .body<Bundle>()
        } returns bundle

        val response =
            locationService.getLocationsByFHIRId(tenant, listOf("12345", "67890", "12345")) // test de-duplicate

        assertEquals(mapOf("12345" to location1.resource, "67890" to location2.resource), response)
    }

    @Test
    fun `getLocationsByFHIRId - chunk works`() {
        val smallLocationService = EpicLocationService(epicClient, 2)
        val tenant = mockk<Tenant>()

        val location1 = mockk<BundleEntry> {
            every { resource } returns mockk<Location> {
                every { id!!.value } returns "12345"
            }
        }
        val location2 = mockk<BundleEntry> {
            every { resource } returns mockk<Location> {
                every { id!!.value } returns "67890"
            }
        }

        val location3 = mockk<BundleEntry> {
            every { resource } returns mockk<Location> {
                every { id!!.value } returns "456"
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
            epicClient.get(tenant, "/api/FHIR/R4/Location", mapOf("_id" to "12345,67890", "_count" to 50))
                .body<Bundle>()
        } returns bundle
        coEvery {
            epicClient.get(tenant, "/api/FHIR/R4/Location", mapOf("_id" to "456", "_count" to 50))
                .body<Bundle>()
        } returns bundle2

        val response =
            smallLocationService.getLocationsByFHIRId(tenant, listOf("12345", "67890", "456"))

        assertEquals(
            mapOf("12345" to location1.resource, "67890" to location2.resource, "456" to location3.resource),
            response
        )
    }
}
