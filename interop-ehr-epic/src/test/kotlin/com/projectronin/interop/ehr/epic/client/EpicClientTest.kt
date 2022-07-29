package com.projectronin.interop.ehr.epic.client

import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.auth.EpicAuthentication
import com.projectronin.interop.ehr.epic.createTestTenant
import com.projectronin.interop.ehr.epic.getClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicClientTest {
    private val validPatientSearchJson = this::class.java.getResource("/ExamplePatientBundle.json")!!.readText()
    private val validAppointmentSearchJson = this::class.java.getResource("/ExampleAppointmentBundle.json")!!.readText()
    private val authenticationBroker = mockk<EHRAuthenticationBroker>()
    private val epicClient = EpicClient(getClient(), authenticationBroker)

    @Test
    fun `throws exception when unable to retrieve authentication during a get operation`() {
        // Set up mock tenant service
        val tenantId = "TEST_EPIC"
        val tenant = createTestTenant(
            clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            serviceEndpoint = "http://www.example.org/never-called",
            privateKey = "testPrivateKey",
            tenantMnemonic = tenantId
        )

        every { authenticationBroker.getAuthentication(tenant) } returns null

        // Execute test
        val exception = assertThrows<IllegalStateException> {
            runBlocking {
                epicClient.get(
                    tenant,
                    "api/FHIR/R4/Patient",
                    mapOf("given" to "givenName", "family" to "familyName", "birthdate" to "birthDate")
                )
            }
        }

        // Validate Response
        assertEquals("Unable to retrieve authentication for TEST_EPIC", exception.message)
    }

    @Test
    fun `ensure get operation returns correctly`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse().setBody(validPatientSearchJson).setHeader("Content-Type", "application/json")
        )
        mockWebServer.start()

        // Set up mock tenant service
        val tenantId = "TEST_EPIC"
        val tenant = createTestTenant(
            clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            serviceEndpoint = mockWebServer.url("/FHIR-test").toString(),
            privateKey = "testPrivateKey",
            tenantMnemonic = tenantId
        )

        val authentication = EpicAuthentication("accessToken", "tokenType", 3600, "scope")
        every { authenticationBroker.getAuthentication(tenant) } returns authentication

        // Execute test
        val response = runBlocking {
            val httpResponse = epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("given" to "givenName", "family" to "familyName", "birthdate" to "birthDate")
            )
            httpResponse.bodyAsText()
        }

        // Validate Response
        assertEquals(validPatientSearchJson, response)
    }

    @Test
    fun `ensure get operation returns correctly with list parameters`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse().setBody(validPatientSearchJson).setHeader("Content-Type", "application/json")
        )
        mockWebServer.start()

        // Set up mock tenant service
        val tenantId = "TEST_EPIC"
        val tenant = createTestTenant(
            clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            serviceEndpoint = mockWebServer.url("/FHIR-test").toString(),
            privateKey = "testPrivateKey",
            tenantMnemonic = tenantId
        )

        val authentication = EpicAuthentication("accessToken", "tokenType", 3600, "scope")
        every { authenticationBroker.getAuthentication(tenant) } returns authentication

        // Execute test
        val response = runBlocking {
            val httpResponse = epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("list" to listOf("one", "two"))
            )
            httpResponse.bodyAsText()
        }

        // Validate Response
        assertEquals(validPatientSearchJson, response)
    }

    @Test
    fun `ensure get operation returns correctly when passed full url`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse().setBody(validPatientSearchJson).setHeader("Content-Type", "application/json")
        )
        mockWebServer.start()

        // Set up mock tenant service
        val tenantId = "TEST_EPIC"
        val tenant = createTestTenant(
            clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            serviceEndpoint = "www.test.com",
            privateKey = "testPrivateKey",
            tenantMnemonic = tenantId
        )

        val authentication = EpicAuthentication("accessToken", "tokenType", 3600, "scope")
        every { authenticationBroker.getAuthentication(tenant) } returns authentication

        // Execute test
        val response = runBlocking {
            val httpResponse = epicClient.get(
                tenant,
                mockWebServer.url("/FHIR-test?sessionId=123").toString()
            )
            httpResponse.bodyAsText()
        }

        // Validate Response
        assertEquals(validPatientSearchJson, response)
    }

    @Test
    fun `ensure get operation handles empty parameters`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse().setBody(validPatientSearchJson).setHeader("Content-Type", "application/json")
        )
        mockWebServer.start()

        // Set up mock tenant service
        val tenantId = "TEST_EPIC"
        val tenant = createTestTenant(
            clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            serviceEndpoint = mockWebServer.url("/FHIR-test").toString(),
            privateKey = "testPrivateKey",
            tenantMnemonic = tenantId
        )

        val authentication = EpicAuthentication("accessToken", "tokenType", 3600, "scope")
        every { authenticationBroker.getAuthentication(tenant) } returns authentication

        // Execute test
        val response = runBlocking {
            val httpResponse = epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient"
            )
            httpResponse.bodyAsText()
        }

        // Validate Response
        assertEquals(validPatientSearchJson, response)
    }

    @Test
    fun `ensure get operation throws exception on bad http status`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.InternalServerError.value)
                .setHeader("Content-Type", "application/json")
        )
        mockWebServer.start()

        // Set up mock tenant service
        val tenantId = "TEST_EPIC"
        val tenant = createTestTenant(
            clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            serviceEndpoint = mockWebServer.url("/FHIR-test").toString(),
            privateKey = "testPrivateKey",
            tenantMnemonic = tenantId
        )

        val authentication = EpicAuthentication("accessToken", "tokenType", 3600, "scope")
        every { authenticationBroker.getAuthentication(tenant) } returns authentication

        // Execute test
        assertThrows<IOException> {
            runBlocking {
                val httpResponse = epicClient.get(
                    tenant,
                    "/api/FHIR/R4/Patient",
                    mapOf("given" to "givenName", "family" to "familyName", "birthdate" to "birthDate")
                )
                httpResponse.bodyAsText()
            }
        }
    }

    @Test
    fun `ensure post operation returns correctly`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse().setBody(validAppointmentSearchJson).setHeader("Content-Type", "application/json")
        )
        mockWebServer.start()

        // Set up mock tenant service
        val tenantId = "TEST_EPIC"
        val tenant = createTestTenant(
            clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            serviceEndpoint = mockWebServer.url("/FHIR-test").toString(),
            privateKey = "testPrivateKey",
            tenantMnemonic = tenantId
        )

        val authentication = EpicAuthentication("accessToken", "tokenType", 3600, "scope")
        every { authenticationBroker.getAuthentication(tenant) } returns authentication

        // Execute test
        val response = runBlocking {
            val httpResponse =
                epicClient.post(
                    tenant,
                    "api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments",
                    GetPatientAppointmentsRequest("1", "1/1/2015", "11/1/2015", "E5597", "EPI")
                )
            httpResponse.bodyAsText()
        }

        // Validate Response
        assertEquals(validAppointmentSearchJson, response)
    }

    @Test
    fun `ensure post operation throws exception on bad http status`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.InternalServerError.value)
                .setHeader("Content-Type", "application/json")
        )
        mockWebServer.start()

        // Set up mock tenant service
        val tenantId = "TEST_EPIC"
        val tenant = createTestTenant(
            clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            serviceEndpoint = mockWebServer.url("/FHIR-test").toString(),
            privateKey = "testPrivateKey",
            tenantMnemonic = tenantId
        )

        val authentication = EpicAuthentication("accessToken", "tokenType", 3600, "scope")
        every { authenticationBroker.getAuthentication(tenant) } returns authentication

        // Execute test
        assertThrows<IOException> {
            runBlocking {
                val httpResponse = epicClient.post(
                    tenant,
                    "api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments",
                    GetPatientAppointmentsRequest("1", "1/1/2015", "11/1/2015", "E5597", "EPI")
                )
                httpResponse.bodyAsText()
            }
        }
    }
}
