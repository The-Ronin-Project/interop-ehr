package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.RequestGroupService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicRequestGroupTest {
    private var epicClient: EpicClient = mockk()
    private var requestGroupService: RequestGroupService = EpicRequestGroupService(epicClient, 5)

    @Test
    fun getRequestGroupByFHIRId() {
        val tenant = mockk<Tenant>()

        val requestGroup1 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "18675309"
                    }
            }
        val requestGroup2 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "98765432"
                    }
            }
        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(requestGroup1, requestGroup2)
                every { link } returns emptyList()
            }

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/RequestGroup",
                mapOf("_id" to listOf("18675309", "98765432")),
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "18675309")

        val response =
            requestGroupService.getRequestGroupByFHIRId(
                tenant,
                listOf("18675309", "98765432", "18675309"),
            ) // test de-duplicate

        assertEquals(mapOf("18675309" to requestGroup1.resource, "98765432" to requestGroup2.resource), response)
    }

    @Test
    fun `chunking works for getByIDs`() {
        val requestGroupOfFour = EpicRequestGroupService(epicClient, 4)
        val tenant = mockk<Tenant>()

        val requestGroup1 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "18675309"
                    }
            }
        val requestGroup2 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "98765432"
                    }
            }
        val requestGroup3 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "11223344"
                    }
            }
        val requestGroup4 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "44332211"
                    }
            }
        val requestGroup5 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "11998877"
                    }
            }
        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(requestGroup1, requestGroup2, requestGroup3, requestGroup4)
                every { link } returns emptyList()
            }
        val bundle2 =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(requestGroup5)
                every { link } returns emptyList()
            }

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/RequestGroup",
                mapOf("_id" to listOf("18675309", "98765432", "11223344", "44332211")),
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "18675309")
        coEvery {
            epicClient.get(tenant, "/api/FHIR/R4/RequestGroup", mapOf("_id" to listOf("11998877")))
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle2 }, "44332211")

        val response =
            requestGroupOfFour.getByIDs(tenant, listOf("18675309", "98765432", "11223344", "44332211", "11998877"))
        assertEquals(
            mapOf(
                "18675309" to requestGroup1.resource,
                "98765432" to requestGroup2.resource,
                "11223344" to requestGroup3.resource,
                "44332211" to requestGroup4.resource,
                "11998877" to requestGroup5.resource,
            ),
            response,
        )
    }

    @Test
    fun `chunking works for getRequestGroupByFHIRId`() {
        val requestGroupOfFour = EpicRequestGroupService(epicClient, 4)
        val tenant = mockk<Tenant>()

        val requestGroup1 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "18675309"
                    }
            }
        val requestGroup2 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "98765432"
                    }
            }
        val requestGroup3 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "11223344"
                    }
            }
        val requestGroup4 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "44332211"
                    }
            }
        val requestGroup5 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<RequestGroup>(relaxed = true) {
                        every { id!!.value } returns "11998877"
                    }
            }
        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(requestGroup1, requestGroup2, requestGroup3, requestGroup4)
                every { link } returns emptyList()
            }
        val bundle2 =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(requestGroup5)
                every { link } returns emptyList()
            }

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/RequestGroup",
                mapOf("_id" to listOf("18675309", "98765432", "11223344", "44332211")),
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "18675309")
        coEvery {
            epicClient.get(tenant, "/api/FHIR/R4/RequestGroup", mapOf("_id" to listOf("11998877")))
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle2 }, "44332211")

        val response =
            requestGroupOfFour.getRequestGroupByFHIRId(
                tenant,
                listOf("18675309", "98765432", "11223344", "44332211", "11998877"),
            )
        assertEquals(
            mapOf(
                "18675309" to requestGroup1.resource,
                "98765432" to requestGroup2.resource,
                "11223344" to requestGroup3.resource,
                "44332211" to requestGroup4.resource,
                "11998877" to requestGroup5.resource,
            ),
            response,
        )
    }
}
