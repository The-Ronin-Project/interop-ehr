package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.util.toListOfType
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

class EpicPagingServiceTest {
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

    private class TestService(epicClient: EpicClient, private val parameters: Map<String, Any?>) :
        EpicPagingService(epicClient) {
        fun getConditions(tenant: Tenant): List<Condition> {
            val bundle = getBundleWithPaging(tenant, "url", parameters)
            return bundle.toListOfType()
        }
    }
}
