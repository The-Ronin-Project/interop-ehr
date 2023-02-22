package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CernerConditionServiceTest {
    private lateinit var cernerClient: CernerClient
    private lateinit var conditionService: CernerConditionService
    private lateinit var httpResponse: HttpResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private val validConditionSearch = readResource<Bundle>("/ExampleConditionBundle.json")
    private val pagingConditionSearch = readResource<Bundle>("/ExampleConditionBundleWithPaging.json")
    private val categorySystem = CodeSystem.CONDITION_CATEGORY.uri.value
    private val clinicalSystem = CodeSystem.CONDITION_CLINICAL.uri.value

    @BeforeEach
    fun setup() {
        cernerClient = mockk()
        httpResponse = mockk()
        pagingHttpResponse = mockk()
        conditionService = CernerConditionService(cernerClient)
    }

    @Test
    fun `make sure conditions are returned`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )
        val patientFhirId = "abc"
        val category = "problem-list-item"
        val clinicalStatus = "active"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            cernerClient.get(
                tenant,
                "/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to category,
                    "clinical-status" to clinicalStatus,
                    "_count" to 20
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
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
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
            cernerClient.get(
                tenant,
                "/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to "problem-list-item,encounter-diagnosis",
                    "clinical-status" to "active",
                    "_count" to 20
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

        // 2 encounter and 1 problem list item
        assertEquals(3, bundle.size)
        assertTrue(
            bundle.any {
                it.category.any { codeableConcept ->
                    codeableConcept.coding.any { coding ->
                        coding.code?.value == "problem-list-item" || coding.code?.value == "encounter-diagnosis"
                    }
                }
            }

        )
    }

    @Test
    fun `ensure multiple clinicalStatus codes are supported`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
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
        val clinicalStatusTokens = "active,resolved"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            cernerClient.get(
                tenant,
                "/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to categoryToken,
                    "clinical-status" to clinicalStatusTokens,
                    "_count" to 20
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
        // 2 active, 1 resolved
        assertEquals(3, bundle.size)
        assertTrue(
            bundle.any {
                it.clinicalStatus?.coding!!.any { coding ->
                    coding.code?.value == "active" || coding.code?.value == "resolved"
                }
            }
        )
    }

    @Test
    fun `ensure category tokens are supported`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )
        val patientFhirId = "abc"
        val categoryToken = "$categorySystem|problem-list-item"
        val clinicalStatusToken = "$clinicalSystem|active"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            cernerClient.get(
                tenant,
                "/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to categoryToken,
                    "clinical-status" to "active",
                    "_count" to 20
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

        assertEquals(3, bundle.size)
    }

    @Test
    fun `ensure clinicalStatus tokens are supported`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )
        val patientFhirId = "abc"
        val categoryToken = "$categorySystem|problem-list-item"
        val clinicalStatusToken = "$clinicalSystem|active"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            cernerClient.get(
                tenant,
                "/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to categoryToken,
                    "clinical-status" to "active",
                    "_count" to 20
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

        assertEquals(3, bundle.size)
    }

    @Test
    fun `ensure paging works`() {
        val patientFhirId = "abc"
        val categoryToken = "$categorySystem|problem-list-item"
        val clinicalStatusToken = "$clinicalSystem|active"
        val nextUrl = "https://fhir-ehr-code.cerner.com/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d/Condition?NEXT"
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        every { pagingHttpResponse.status } returns HttpStatusCode.OK
        coEvery { pagingHttpResponse.body<Bundle>() } returns pagingConditionSearch
        coEvery {
            cernerClient.get(
                tenant,
                "/Condition",
                mapOf(
                    "patient" to patientFhirId,
                    "category" to categoryToken,
                    "clinical-status" to "active",
                    "_count" to 20
                )
            )
        } returns pagingHttpResponse

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validConditionSearch
        coEvery {
            cernerClient.get(
                tenant,
                nextUrl,
            )
        } returns httpResponse

        val list =
            conditionService.findConditions(
                tenant,
                patientFhirId,
                categoryToken,
                clinicalStatusToken
            )

        assertEquals(9, list.size)
    }
}
