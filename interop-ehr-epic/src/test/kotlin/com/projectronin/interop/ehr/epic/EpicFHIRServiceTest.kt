package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
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
    private lateinit var ehrResponse: EHRResponse
    private val conditionBundle = readResource<Bundle>("/ExampleConditionBundle.json")

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
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
        } returns ehrResponse

        val service = TestService(epicClient, mapOf())
        val conditions = service.getConditions(tenant)
        assertEquals(7, conditions.size)
    }

    @Test
    fun `getByIds works`() {
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
                    "_id" to listOf(
                        "eGVC1DSR9YDJxMi7Th3xbsA3",
                        "eLn-6z3hP2NJG0T20kjWyGw3",
                        "ehbL8Q9rn3jAsl6s8yGxC1w3",
                        "e.3Yf2fdBeJFpNyIZg0GVFg3",
                        "eqrR1BMfXhGmimr8OUK7PbA3",
                        "eZ1muPaohtiRvQlo0D-yxEg3",
                        "e67ikTL29RrR6zDPXYcbWBA3"
                    )
                )
            )
        } returns ehrResponse

        val service = TestService(epicClient, mapOf())
        val example = service.getByIDs(
            tenant,
            listOf(
                "eGVC1DSR9YDJxMi7Th3xbsA3",
                "eLn-6z3hP2NJG0T20kjWyGw3",
                "ehbL8Q9rn3jAsl6s8yGxC1w3",
                "e.3Yf2fdBeJFpNyIZg0GVFg3",
                "eqrR1BMfXhGmimr8OUK7PbA3",
                "eZ1muPaohtiRvQlo0D-yxEg3",
                "e67ikTL29RrR6zDPXYcbWBA3"
            )
        )
        assertEquals(7, example.size)
        assertEquals("eGVC1DSR9YDJxMi7Th3xbsA3", example["eGVC1DSR9YDJxMi7Th3xbsA3"]?.id?.value)
        assertEquals("eLn-6z3hP2NJG0T20kjWyGw3", example["eLn-6z3hP2NJG0T20kjWyGw3"]?.id?.value)
        assertEquals("ehbL8Q9rn3jAsl6s8yGxC1w3", example["ehbL8Q9rn3jAsl6s8yGxC1w3"]?.id?.value)
        assertEquals("e.3Yf2fdBeJFpNyIZg0GVFg3", example["e.3Yf2fdBeJFpNyIZg0GVFg3"]?.id?.value)
        assertEquals("eqrR1BMfXhGmimr8OUK7PbA3", example["eqrR1BMfXhGmimr8OUK7PbA3"]?.id?.value)
        assertEquals("eZ1muPaohtiRvQlo0D-yxEg3", example["eZ1muPaohtiRvQlo0D-yxEg3"]?.id?.value)
        assertEquals("e67ikTL29RrR6zDPXYcbWBA3", example["e67ikTL29RrR6zDPXYcbWBA3"]?.id?.value)
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
        } returns ehrResponse

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

        val condition = mockk<Condition>(relaxed = true)
        val bundle = mockk<Bundle>(relaxed = true) {
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
        } returns ehrResponse

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

        val condition = mockk<Condition>(relaxed = true)
        val bundle = mockk<Bundle>(relaxed = true) {
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
        } returns ehrResponse

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

        val condition = mockk<Condition>(relaxed = true)
        val bundle = mockk<Bundle>(relaxed = true) {
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
        } returns ehrResponse

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

        val condition = mockk<Condition>(relaxed = true)
        val bundle = mockk<Bundle>(relaxed = true) {
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
        } returns ehrResponse

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

        val condition = mockk<Condition>(relaxed = true)
        val bundle = mockk<Bundle>(relaxed = true) {
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
        } returns ehrResponse

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

        val condition1 = mockk<Condition>(relaxed = true) {
            every { id } returns Id("1234")
        }
        val condition2 = mockk<Condition>(relaxed = true) {
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
        } returns ehrResponse

        val httpResponse2 = mockk<HttpResponse> {
            every { status } returns HttpStatusCode.OK
            coEvery { body<Bundle>() } returns bundle2
        }

        coEvery { epicClient.get(tenant, "http://test/1234") } returns EHRResponse(httpResponse2, "67890")

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
