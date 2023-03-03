package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Condition
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

class EpicFHIRServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var httpResponse: HttpResponse
    private val conditionBundle = readResource<Bundle>("/ExampleConditionBundle.json")

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
    }

    @Test
    fun `ensure standard parameters are added when missing`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns conditionBundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50
                )
            )
        } returns httpResponse

        val service = TestService(epicClient, mapOf())
        val conditions = service.getConditions(tenant)
        assertEquals(7, conditions.size)
    }

    @Test
    fun `ensure standard parameters are not included when already provided`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns conditionBundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 250
                )
            )
        } returns httpResponse

        val service = TestService(epicClient, mapOf("_count" to 250))
        val conditions = service.getConditions(tenant)
        assertEquals(7, conditions.size)
    }

    @Test
    fun `ensure bundle handles no links`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        val condition = mockk<Condition>()
        val bundle = mockk<Bundle> {
            every { link } returns listOf()
            every { entry } returns listOf(
                mockk {
                    every { resource } returns condition
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50
                )
            )
        } returns httpResponse

        val service = TestService(epicClient, mapOf())
        val conditions = service.getConditions(tenant)
        assertEquals(1, conditions.size)
    }

    @Test
    fun `ensure bundle handles no next links`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        val condition = mockk<Condition>()
        val bundle = mockk<Bundle> {
            every { link } returns listOf(
                mockk {
                    every { relation } returns FHIRString("self")
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns condition
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50
                )
            )
        } returns httpResponse

        val service = TestService(epicClient, mapOf())
        val conditions = service.getConditions(tenant)
        assertEquals(1, conditions.size)
    }

    @Test
    fun `ensure bundle handles links with no relations`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        val condition = mockk<Condition>()
        val bundle = mockk<Bundle> {
            every { link } returns listOf(
                mockk {
                    every { relation } returns null
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns condition
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50
                )
            )
        } returns httpResponse

        val service = TestService(epicClient, mapOf())
        val conditions = service.getConditions(tenant)
        assertEquals(1, conditions.size)
    }

    @Test
    fun `ensure bundle handles next link with no URL`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        val condition = mockk<Condition>()
        val bundle = mockk<Bundle> {
            every { link } returns listOf(
                mockk {
                    every { relation } returns FHIRString("next")
                    every { url } returns null
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns condition
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50
                )
            )
        } returns httpResponse

        val service = TestService(epicClient, mapOf())
        val conditions = service.getConditions(tenant)
        assertEquals(1, conditions.size)
    }

    @Test
    fun `ensure bundle handles next link with URL with no value`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        val condition = mockk<Condition>()
        val bundle = mockk<Bundle> {
            every { link } returns listOf(
                mockk {
                    every { relation } returns FHIRString("next")
                    every { url } returns Uri(null)
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns condition
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50
                )
            )
        } returns httpResponse

        val service = TestService(epicClient, mapOf())
        val conditions = service.getConditions(tenant)
        assertEquals(1, conditions.size)
    }

    @Test
    fun `ensure bundle handles next link with URL`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        val condition1 = mockk<Condition>() {
            every { id } returns Id("1234")
        }
        val condition2 = mockk<Condition>() {
            every { id } returns Id("5678")
        }
        val bundle1 = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf(
                mockk {
                    every { relation } returns FHIRString("next")
                    every { url } returns Uri("http://test/1234")
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns condition1
                }
            )
        }
        val bundle2 = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf()
            every { entry } returns listOf(
                mockk {
                    every { resource } returns condition2
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle1
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50
                )
            )
        } returns httpResponse

        val httpResponse2 = mockk<HttpResponse> {
            every { status } returns HttpStatusCode.OK
            coEvery { body<Bundle>() } returns bundle2
        }

        coEvery { epicClient.get(tenant, "http://test/1234") } returns httpResponse2

        val service = TestService(epicClient, mapOf())
        val conditions = service.getConditions(tenant)
        assertEquals(2, conditions.size)
    }

    private class TestService(
        epicClient: EpicClient,
        private val parameters: Map<String, Any?>,
        override val fhirURLSearchPart: String = "url",
        override val fhirResourceType: Class<Condition> = Condition::class.java
    ) :
        EpicFHIRService<Condition>(epicClient) {
        fun getConditions(tenant: Tenant): List<Condition> {
            return getResourceListFromSearch(tenant, parameters)
        }
    }
}
