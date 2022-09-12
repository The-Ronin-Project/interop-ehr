package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.resource.Bundle
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EpicConditionServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var conditionService: EpicConditionService
    private lateinit var httpResponse: HttpResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private val validConditionSearch = readResource<Bundle>("/ExampleConditionBundle.json")
    private val pagingConditionSearch = readResource<Bundle>("/ExampleConditionBundleWithPaging.json")
    private val categorySystem = CodeSystem.CONDITION_CATEGORY.uri.value
    private val clinicalSystem = CodeSystem.CONDITION_CLINICAL.uri.value

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
        pagingHttpResponse = mockk()
        conditionService = EpicConditionService(epicClient)
    }

    @Test
    fun `ensure conditions are returned`() {
        val tenant =
            createTestTenant(
                "clientId",
                "https://example.org",
                "testPrivateKey",
                "tenantId"
            )
        val patientFhirId = "abc"
        val category = "problem-list-item"
        val clinicalStatus = "active"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to category,
                    "clinical-status" to clinicalStatus
                )
            )
        } returns httpResponse

        val bundle =
            conditionService.findConditions(
                tenant,
                patientFhirId,
                category,
                clinicalStatus
            )

        assertEquals(validConditionSearch.entry.map { it.resource }, bundle)
    }

    @Test
    fun `ensure multiple category codes are supported`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )
        val patientFhirId = "abc"
        val categoryCodes = listOf(
            FHIRSearchToken(code = "problem-list-item"),
            FHIRSearchToken(code = "encounter-diagnosis")
        )
        val clinicalStatusCodes = listOf(
            FHIRSearchToken(code = "active")
        )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to "problem-list-item,encounter-diagnosis",
                    "clinical-status" to "active"
                )
            )
        } returns httpResponse

        val bundle =
            conditionService.findConditionsByCodes(
                tenant,
                patientFhirId,
                categoryCodes,
                clinicalStatusCodes
            )

        // 1 patient had 7 problem-list-item conditions and 0 encounter-diagnosis conditions
        assertEquals(7, bundle.size)
    }

    @Test
    fun `ensure multiple clinicalStatus codes are supported`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )
        val patientFhirId = "abc"
        val categoryCodes = listOf(
            FHIRSearchToken(system = categorySystem, code = "problem-list-item")
        )
        val categoryToken = "$categorySystem|problem-list-item"
        val clinicalStatusCodes = listOf(
            FHIRSearchToken(system = clinicalSystem, code = "active"),
            FHIRSearchToken(system = clinicalSystem, code = "resolved")
        )
        val clinicalStatusTokens = "$clinicalSystem|active,$clinicalSystem|resolved"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to categoryToken,
                    "clinical-status" to clinicalStatusTokens
                )
            )
        } returns httpResponse

        val bundle =
            conditionService.findConditionsByCodes(
                tenant,
                patientFhirId,
                categoryCodes,
                clinicalStatusCodes
            )

        // 1 patient had 7 active conditions and 0 resolved conditions
        assertEquals(7, bundle.size)
    }

    @Test
    fun `ensure category tokens are supported`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )
        val patientFhirId = "abc"
        val categoryToken = "$categorySystem|problem-list-item"
        val clinicalStatusToken = "$clinicalSystem|active"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to categoryToken,
                    "clinical-status" to clinicalStatusToken
                )
            )
        } returns httpResponse

        val bundle =
            conditionService.findConditions(
                tenant,
                patientFhirId,
                categoryToken,
                clinicalStatusToken
            )

        // 1 patient had 7 problem-list-item conditions
        assertEquals(7, bundle.size)
    }

    @Test
    fun `ensure clinicalStatus tokens are supported`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )
        val patientFhirId = "abc"
        val categoryToken = "$categorySystem|problem-list-item"
        val clinicalStatusToken = "$clinicalSystem|active"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to categoryToken,
                    "clinical-status" to clinicalStatusToken
                )
            )
        } returns httpResponse

        val bundle =
            conditionService.findConditions(
                tenant,
                patientFhirId,
                categoryToken,
                clinicalStatusToken
            )

        // 1 patient had 7 problem-list-item conditions
        assertEquals(7, bundle.size)
    }

    @Test
    fun `ensure paging works`() {
        val patientFhirId = "abc"
        val categoryToken = "$categorySystem|problem-list-item"
        val clinicalStatusToken = "$clinicalSystem|active"
        val tenant =
            createTestTenant(
                "clientId",
                "https://example.org",
                "testPrivateKey",
                "tenantId"
            )

        // Mock response with paging
        every { pagingHttpResponse.status } returns HttpStatusCode.OK
        coEvery { pagingHttpResponse.body<Bundle>() } returns pagingConditionSearch
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to categoryToken,
                    "clinical-status" to clinicalStatusToken
                )
            )
        } returns pagingHttpResponse

        // Mock response without paging
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            epicClient.get(
                tenant,
                "https://apporchard.epic.com/interconnect-aocurprd-oauth/api/FHIR/R4/Condition?patient=eovSKnwDlsv-8MsEzCJO3BA3&clinical-status=active,inactive,resolved&category=problem-list-item",
            )
        } returns httpResponse

        val list =
            conditionService.findConditions(
                tenant,
                patientFhirId,
                categoryToken,
                clinicalStatusToken
            )

        // 7 resources from the first query, 7 from the second
        assertEquals(14, list.size)
    }
}
