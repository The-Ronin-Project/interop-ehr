package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicConditionBundle
import io.ktor.client.call.receive
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicConditionServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var conditionService: EpicConditionService
    private lateinit var httpResponse: HttpResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private val validConditionSearchJSON = this::class.java.getResource("/ExampleConditionBundle.json")!!.readText()
    private val pagingConditionSearchJSON = this::class.java.getResource("/ExampleConditionBundleWithPaging.json")!!.readText()

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
        pagingHttpResponse = mockk()
        conditionService = EpicConditionService(epicClient)
    }

    @Test
    fun `ensure practitioners are returned`() {
        val patientFhirId = "fhirId"
        val conditionCategoryCode = "catCode"
        val clinicalStatus = "clinicalStatus"

        val tenant =
            createTestTenant(
                "clientId",
                "https://example.org",
                "testPrivateKey",
                "tenantId"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<String>() } returns validConditionSearchJSON
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to conditionCategoryCode,
                    "clinical-status" to clinicalStatus
                )
            )
        } returns httpResponse

        val bundle =
            conditionService.findConditions(
                tenant,
                patientFhirId,
                conditionCategoryCode,
                clinicalStatus
            )

        assertEquals(EpicConditionBundle(validConditionSearchJSON), bundle)
    }

    @Test
    fun `ensure http error handled`() {
        val patientFhirId = "fhirId"
        val conditionCategoryCode = "catCode"
        val clinicalStatus = "clinicalStatus"

        val tenant =
            createTestTenant(
                "clientId",
                "https://example.org",
                "testPrivateKey",
                "tenantId"
            )

        every { httpResponse.status } returns HttpStatusCode.NotFound
        coEvery { httpResponse.receive<String>() } returns validConditionSearchJSON
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to conditionCategoryCode,
                    "clinical-status" to clinicalStatus
                )
            )
        } returns httpResponse

        assertThrows<IOException> {
            conditionService.findConditions(
                tenant,
                patientFhirId,
                conditionCategoryCode,
                clinicalStatus
            )
        }
    }

    @Test
    fun `ensure paging works`() {
        val patientFhirId = "fhirId"
        val conditionCategoryCode = "catCode"
        val clinicalStatus = "clinicalStatus"

        val tenant =
            createTestTenant(
                "clientId",
                "https://example.org",
                "testPrivateKey",
                "tenantId"
            )

        // Mock response with paging
        every { pagingHttpResponse.status } returns HttpStatusCode.OK
        coEvery { pagingHttpResponse.receive<String>() } returns pagingConditionSearchJSON
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to conditionCategoryCode,
                    "clinical-status" to clinicalStatus
                )
            )
        } returns pagingHttpResponse

        // Mock response without paging
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<String>() } returns validConditionSearchJSON
        coEvery {
            epicClient.get(
                tenant,
                "https://apporchard.epic.com/interconnect-aocurprd-oauth/api/FHIR/R4/Condition?patient=eovSKnwDlsv-8MsEzCJO3BA3&clinical-status=active,inactive,resolved&category=problem-list-item",
            )
        } returns httpResponse

        val bundle =
            conditionService.findConditions(
                tenant,
                patientFhirId,
                conditionCategoryCode,
                clinicalStatus
            )

        // 7 resources from the first query, 7 from the second
        assertEquals(14, bundle.resources.size)
    }
}
