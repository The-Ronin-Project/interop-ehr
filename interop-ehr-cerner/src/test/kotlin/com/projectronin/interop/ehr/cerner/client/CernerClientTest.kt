package com.projectronin.interop.ehr.cerner.client

import com.projectronin.interop.common.http.exceptions.ServerFailureException
import com.projectronin.interop.ehr.cerner.auth.CernerAuthentication
import com.projectronin.interop.ehr.cerner.auth.CernerAuthenticationService
import com.projectronin.interop.ehr.cerner.createTestTenant
import com.projectronin.interop.ehr.cerner.getClient
import io.ktor.client.statement.bodyAsText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CernerClientTest {
    private val patientResult = this::class.java.getResource("/ExamplePatientResponse.json")!!.readText()
    private val authenticationBroker = mockk<CernerAuthenticationService>()
    private val cernerClient = CernerClient(getClient(), authenticationBroker)
    private lateinit var mockWebServer: MockWebServer

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `throws exception when unable to retrieve authentication during a get operation`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                serviceEndpoint = mockWebServer.url("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d").toString(),
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )
        val authentication = CernerAuthentication("accessToken", "Bearer", 570, "system/Patient.read", "")
        every { runBlocking { authenticationBroker.getAuthentication(tenant) } } returns authentication

        val response = assertThrows<ServerFailureException> {
            runBlocking {
                cernerClient.get(
                    tenant,
                    "/Patient/12724066",

                )
            }
        }
    }
    @Test
    fun `ensure get operation returns correctly`() {
        mockWebServer.enqueue(
            MockResponse().setBody(patientResult).setHeader("Content-Type", "application/json")
        )
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                serviceEndpoint = mockWebServer.url("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d").toString(),
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        val authentication = CernerAuthentication("accessToken", "Bearer", 570, "system/Patient.read", "")
        every { runBlocking { authenticationBroker.getAuthentication(tenant) } } returns authentication

        val response = runBlocking {
            val httpResponse = cernerClient.get(
                tenant,
                "/Patient/12724066"
            )
            httpResponse.bodyAsText()
        }
        assertEquals(patientResult, response)
    }
}
