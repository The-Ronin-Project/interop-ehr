package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicFindPractitionersResponse
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

class EpicPractitionerServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var practitionerService: EpicPractitionerService
    private lateinit var httpResponse: HttpResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private val validPractitionerSearchJSON =
        this::class.java.getResource("/ExampleFindPractitionersResponse.json")!!.readText()
    private val pagingPractitionerSearchJSON =
        this::class.java.getResource("/ExampleFindPractitionersResponseWithPaging.json")!!.readText()

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
        pagingHttpResponse = mockk()
        practitionerService = EpicPractitionerService(epicClient)
    }

    @Test
    fun `ensure practitioners are returned`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<String>() } returns validPractitionerSearchJSON
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to "PractitionerRole:practitioner",
                    "location" to "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03"
                )
            )
        } returns httpResponse

        val bundle =
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03")
            )

        assertEquals(EpicFindPractitionersResponse(validPractitionerSearchJSON), bundle)
    }

    @Test
    fun `ensure http error handled`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.NotFound
        coEvery { httpResponse.receive<String>() } returns validPractitionerSearchJSON
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to "PractitionerRole:practitioner",
                    "location" to "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03"
                )
            )
        } returns httpResponse

        assertThrows<IOException> {
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03")
            )
        }
    }

    @Test
    fun `ensure multiple locations are supported`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<String>() } returns validPractitionerSearchJSON
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf("_include" to "PractitionerRole:practitioner", "location" to "abc")
            )
        } returns httpResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf("_include" to "PractitionerRole:practitioner", "location" to "123")
            )
        } returns httpResponse

        val bundle =
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("abc", "123")
            )

        // 64 for each location
        assertEquals(128, bundle.practitionerRoles!!.resources.size)

        // 64 total because duplicate practitioners are removed
        assertEquals(64, bundle.practitioners.resources.size)
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

        // Mock response with paging
        every { pagingHttpResponse.status } returns HttpStatusCode.OK
        coEvery { pagingHttpResponse.receive<String>() } returns pagingPractitionerSearchJSON
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/PractitionerRole",
                mapOf(
                    "_include" to "PractitionerRole:practitioner",
                    "location" to "e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03"
                )
            )
        } returns pagingHttpResponse

        // Mock response without paging
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<String>() } returns validPractitionerSearchJSON
        coEvery {
            epicClient.get(
                tenant,
                "https://apporchard.epic.com/interconnect-aocurprd-oauth/api/FHIR/R4/PractitionerRole?_include=PractitionerRole:practitioner&location=e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03&sessionID=10-57E8BB9A4D4211EC94270050568B7BE6"
            )
        } returns httpResponse

        val bundle =
            practitionerService.findPractitionersByLocation(
                tenant,
                listOf("e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03")
            )

        // 2 Resources from the first query, 64 from the second
        assertEquals(66, bundle.practitionerRoles?.resources?.size)

        // 2 of the practitioners are duplicates and get filtered out
        assertEquals(64, bundle.practitioners.resources.size)
    }
}
