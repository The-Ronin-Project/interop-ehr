package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.valueset.ObservationCategoryCodes
import com.projectronin.interop.tenant.config.data.TenantCodesDAO
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EpicObservationServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var observationService: EpicObservationService
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private lateinit var codesDAO: TenantCodesDAO
    private val validObservationSearchBundle = readResource<Bundle>("/ExampleObservationBundle.json")
    private val pagingObservationSearchBundle = readResource<Bundle>("/ExampleObservationBundleWithPaging.json")
    private val categorySystem = CodeSystem.OBSERVATION_CATEGORY.uri.value
    private val pastDate = LocalDate.now().minusDays(60).toString()

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
        pagingHttpResponse = mockk()
        codesDAO = mockk()
        observationService = EpicObservationService(epicClient, 1, codesDAO, 60)
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
                    "date" to "ge$pastDate",
                    "_count" to 50
                )
            )
        } returns ehrResponse

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
                    "date" to "ge$pastDate",
                    "_count" to 50
                )
            )
        } returns ehrResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "123",
                    "category" to "social-history",
                    "date" to "ge$pastDate",
                    "_count" to 50
                )
            )
        } returns ehrResponse

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
                    "date" to "ge$pastDate",
                    "_count" to 50
                )
            )
        } returns ehrResponse

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
    fun `ensure multiple category codes are supported new API`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )
        val categoryCodes = listOf(
            ObservationCategoryCodes.SOCIAL_HISTORY,
            ObservationCategoryCodes.LABORATORY
        )
        val categoryTokens = "social-history,laboratory"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validObservationSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "abc",
                    "category" to categoryTokens,
                    "date" to "ge$pastDate",
                    "_count" to 50
                )
            )
        } returns ehrResponse

        val bundle =
            observationService.findObservationsByCategory(
                tenant,
                listOf("abc"),
                categoryCodes
            )

        // 1 patient had 4 social-history observations and 0 laboratory observations
        assertEquals(4, bundle.size)
    }

    @Test
    fun `ensure multiple category codes are supported new API and extra codes`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )
        val categoryCodes = listOf(
            ObservationCategoryCodes.VITAL_SIGNS,
            ObservationCategoryCodes.LABORATORY
        )
        val categoryTokens = "vital-signs,laboratory"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validObservationSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "abc",
                    "category" to categoryTokens,
                    "date" to "ge$pastDate",
                    "_count" to 50
                )
            )
        } returns ehrResponse
        val httpResponse2 = mockk<HttpResponse> {
            every { status } returns HttpStatusCode.OK
        }
        coEvery { httpResponse2.body<Bundle>() } returns validObservationSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Observation",
                mapOf(
                    "patient" to "abc",
                    "code" to "12345,23456",
                    "date" to "ge$pastDate",
                    "_count" to 50
                )
            )
        } returns EHRResponse(httpResponse2, "67890")

        every {
            codesDAO.getByTenantMnemonic("TEST_TENANT")
        } returns mockk {
            every { bmiCode } returns "12345"
            every { bsaCode } returns "23456"
        }
        val bundle =
            observationService.findObservationsByCategory(
                tenant,
                listOf("abc"),
                categoryCodes
            )

        // 1 patient had 4 social-history observations and 0 laboratory observations
        assertEquals(8, bundle.size)
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
                    "date" to "ge$pastDate",
                    "_count" to 50
                )
            )
        } returns ehrResponse

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
                    "date" to "ge$pastDate",
                    "_count" to 50
                )
            )
        } returns EHRResponse(pagingHttpResponse, "67890")

        // Mock response without paging
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validObservationSearchBundle
        coEvery {
            epicClient.get(
                tenant,
                "https://apporchard.epic.com/interconnect-aocurprd-oauth/api/FHIR/R4/Observation?patient=em2zwhHegmZEu39N4dUEIYA3&category=social-history&date=ge2023-02-20&sessionID=10-57E8BB9A4D4211EC94270050568B7BE6"
            )
        } returns ehrResponse

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
