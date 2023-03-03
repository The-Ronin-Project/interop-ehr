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

class EpicObservationServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var observationService: EpicObservationService
    private lateinit var httpResponse: HttpResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private val validObservationSearchBundle = readResource<Bundle>("/ExampleObservationBundle.json")
    private val pagingObservationSearchBundle = readResource<Bundle>("/ExampleObservationBundleWithPaging.json")
    private val categorySystem = CodeSystem.OBSERVATION_CATEGORY.uri.value

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
        pagingHttpResponse = mockk()
        observationService = EpicObservationService(epicClient, 1)
    }

    @Test
    fun `ensure observations are returned`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validObservationSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "em2zwhHegmZEu39N4dUEIYA3",
                    "category" to "social-history",
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            observationService.findObservationsByPatient(
                tenant,
                listOf("em2zwhHegmZEu39N4dUEIYA3"),
                listOf("social-history")
            )

        val expectedObservationBundle = (validObservationSearchBundle).entry.map { it.resource }
        assertEquals(expectedObservationBundle, bundle)
        assertEquals(4, bundle.size)
    }

    @Test
    fun `ensure multiple patients are supported`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validObservationSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "abc",
                    "category" to "social-history",
                    "_count" to 50
                )
            )
        } returns httpResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "123",
                    "category" to "social-history",
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            observationService.findObservationsByPatient(
                tenant,
                listOf("abc", "123"),
                listOf("social-history")
            )

        // each of 2 patients had 4 social-history observations
        assertEquals(8, bundle.size)
    }

    @Test
    fun `ensure multiple patients are supported with batching`() {
        val batchingObservationService = EpicObservationService(epicClient, 2)

        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validObservationSearchBundle

        /*
        Uncomment when we are no longer forcing batchSize to 1 in EpicObservation and remove the 3 temporary cases below
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "loc1,loc2,loc3",
                    "category" to "social-history"
                )
            )
        } returns httpResponse
         */
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "loc1",
                    "category" to "social-history",
                    "_count" to 50
                )
            )
        } returns httpResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "loc2",
                    "category" to "social-history",
                    "_count" to 50
                )
            )
        } returns httpResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "loc3",
                    "category" to "social-history",
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            batchingObservationService.findObservationsByPatient(
                tenant,
                listOf("loc1", "loc2", "loc3"),
                listOf("social-history")
            )

        // each of 3 patients had 4 social-history observations
        assertEquals(12, bundle.size)
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
        val categoryCodes = listOf(
            FHIRSearchToken(system = categorySystem, code = "social-history"),
            FHIRSearchToken(system = categorySystem, code = "laboratory")
        )
        val categoryTokens = "$categorySystem|social-history,$categorySystem|laboratory"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validObservationSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "abc",
                    "category" to categoryTokens,
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            observationService.findObservationsByPatientAndCategory(
                tenant,
                listOf("abc"),
                categoryCodes
            )

        // 1 patient had 4 social-history observations and 0 laboratory observations
        assertEquals(4, bundle.size)
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
        val categoryCodes = listOf(
            FHIRSearchToken(system = categorySystem, code = "social-history")
        )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validObservationSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "abc",
                    "category" to "$categorySystem|social-history",
                    "_count" to 50
                )
            )
        } returns httpResponse

        val bundle =
            observationService.findObservationsByPatientAndCategory(
                tenant,
                listOf("abc"),
                categoryCodes
            )

        // 1 patient had 4 social-history observations
        assertEquals(4, bundle.size)
    }

    @Test
    fun `ensure paging works`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )
        val categoryCodes = listOf(
            FHIRSearchToken(system = categorySystem, code = "social-history")
        )

        // Mock response with paging
        every { pagingHttpResponse.status } returns HttpStatusCode.OK
        coEvery { pagingHttpResponse.body<Bundle>() } returns pagingObservationSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "em2zwhHegmZEu39N4dUEIYA3",
                    "category" to "$categorySystem|social-history",
                    "_count" to 50
                )
            )
        } returns pagingHttpResponse

        // Mock response without paging
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validObservationSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "https://apporchard.epic.com/interconnect-aocurprd-oauth/api/FHIR/R4/Observation?patient=em2zwhHegmZEu39N4dUEIYA3&category=social-history&sessionID=10-57E8BB9A4D4211EC94270050568B7BE6"
            )
        } returns httpResponse

        val bundle =
            observationService.findObservationsByPatientAndCategory(
                tenant,
                listOf("em2zwhHegmZEu39N4dUEIYA3"),
                categoryCodes
            )

        // 4 observations on the first page + 2 observations on the second page, remove duplicates = 4
        assertEquals(4, bundle.size)
    }
}
