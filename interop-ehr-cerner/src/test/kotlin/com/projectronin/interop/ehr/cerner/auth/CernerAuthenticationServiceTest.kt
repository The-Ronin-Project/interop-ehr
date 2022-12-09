package com.projectronin.interop.ehr.cerner.auth

import com.projectronin.interop.common.http.exceptions.ServerFailureException
import com.projectronin.interop.ehr.cerner.createTestTenant
import com.projectronin.interop.ehr.cerner.getClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CernerAuthenticationServiceTest {
    private val authenticationService = CernerAuthenticationService(getClient())
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
    fun `handles auth request`() {

        val responseJson = """
            |{
            |    "access_token": "accesstoken",
            |    "scope": "system/Observation.read",
            |    "token_type": "Bearer",
            |    "expires_in": 570
            |}
        """.trimMargin()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "${mockWebServer.url("/protocols")}/oauth2/profiles/smart-v1/token",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )
        val authResponse = runBlocking { authenticationService.getAuthentication(tenant) }

        assertEquals("system/Observation.read", authResponse?.scope)
        assertNotNull(authResponse?.expiresAt)
        assertEquals("Bearer", authResponse?.tokenType)
        assertEquals(
            "accesstoken",
            authResponse?.accessToken
        )
    }

    @Test
    fun `handles exception from auth request`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "${mockWebServer.url("/protocols")}/oauth2/profiles/smart-v1/token",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        val response = assertThrows<ServerFailureException> {
            runBlocking { authenticationService.getAuthentication(tenant) }
        }
    }
}
