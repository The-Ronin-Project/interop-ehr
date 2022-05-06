package com.projectronin.interop.ehr.epic.auth

import com.projectronin.interop.ehr.epic.createTestTenant
import com.projectronin.interop.ehr.epic.getClient
import io.ktor.client.plugins.ServerResponseException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicAuthenticationServiceTest {
    private val mockWebServer = MockWebServer()
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()
    private val authenticationService = EpicAuthenticationService(getClient())

    @Test
    fun `handles exception thrown by auth request`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val tenant =
            createTestTenant(
                clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                authEndpoint = "${mockWebServer.url("/interconnect-aocurprd-oauth")}/oauth2/token",
                privateKey = testPrivateKey,
                tenantMnemonic = "TestTenant"
            )

        assertThrows<ServerResponseException> {
            authenticationService.getAuthentication(tenant)
        }
    }

    @Test
    fun `formatted private key can be used`() {
        // Set up mock web service
        val responseJson = """
            |{
            |  "access_token": "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y_-zBvt2kw3xn-MY3gY4lOXPee6iKPw3JncYBT1Y-kdPpBYl-lsmUlA4x5dUVC1qbjEi1OHfe_Oa-VRUAeabnMLjYgKI7b",
            |  "token_type": "bearer",
            |  "expires_in": 3600,
            |  "scope": "Patient.read Patient.search"
            |}
        """.trimMargin()

        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))
        mockWebServer.start()

        val tenant =
            createTestTenant(
                clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                authEndpoint = "${mockWebServer.url("/interconnect-aocurprd-oauth")}/oauth2/token",
                privateKey = testPrivateKey,
                tenantMnemonic = "TestTenant"
            )

        // Execute test
        val authentication = authenticationService.getAuthentication(tenant)!!

        // Validate Response
        assertEquals("Patient.read Patient.search", authentication.scope)
        assertNotNull(authentication.expiresAt)
        assertEquals("bearer", authentication.tokenType)
        assertEquals(
            "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y_-zBvt2kw3xn-MY3gY4lOXPee6iKPw3JncYBT1Y-kdPpBYl-lsmUlA4x5dUVC1qbjEi1OHfe_Oa-VRUAeabnMLjYgKI7b",
            authentication.accessToken
        )

        // Validate Request
        val expectedRequestBody = "grant_type=client_credentials&" +
            "client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer&" +
            "client_assertion="
        val actualRequest = mockWebServer.takeRequest()
        assertEquals(
            "application/x-www-form-urlencoded; charset=UTF-8",
            actualRequest.getHeader("Content-Type")
        )
        assertEquals("application/json", actualRequest.getHeader("Accept"))
        val actualRequestBody = actualRequest.body.readUtf8()
        // Check just a portion of the actual request as the end will be different with each encoding
        assertEquals(expectedRequestBody, actualRequestBody.take(expectedRequestBody.length))
    }

    @Test
    fun `formatted private key with spaces can be used`() {
        // Set up mock web service
        val responseJson = """
            |{
            |  "access_token": "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y_-zBvt2kw3xn-MY3gY4lOXPee6iKPw3JncYBT1Y-kdPpBYl-lsmUlA4x5dUVC1qbjEi1OHfe_Oa-VRUAeabnMLjYgKI7b",
            |  "token_type": "bearer",
            |  "expires_in": 3600,
            |  "scope": "Patient.read Patient.search"
            |}
        """.trimMargin()

        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))
        mockWebServer.start()

        val spacedPrivateKey = this::class.java.getResource("/SpacedTestPrivateKey.txt")!!.readText()
        val tenant =
            createTestTenant(
                clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                authEndpoint = "${mockWebServer.url("/interconnect-aocurprd-oauth")}/oauth2/token",
                privateKey = spacedPrivateKey,
                tenantMnemonic = "TestTenant"
            )

        // Execute test
        val authentication = authenticationService.getAuthentication(tenant)!!

        // Validate Response
        assertEquals("Patient.read Patient.search", authentication.scope)
        assertNotNull(authentication.expiresAt)
        assertEquals("bearer", authentication.tokenType)
        assertEquals(
            "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y_-zBvt2kw3xn-MY3gY4lOXPee6iKPw3JncYBT1Y-kdPpBYl-lsmUlA4x5dUVC1qbjEi1OHfe_Oa-VRUAeabnMLjYgKI7b",
            authentication.accessToken
        )

        // Validate Request
        val expectedRequestBody = "grant_type=client_credentials&" +
            "client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer&" +
            "client_assertion="
        val actualRequest = mockWebServer.takeRequest()
        assertEquals(
            "application/x-www-form-urlencoded; charset=UTF-8",
            actualRequest.getHeader("Content-Type")
        )
        assertEquals("application/json", actualRequest.getHeader("Accept"))
        val actualRequestBody = actualRequest.body.readUtf8()
        // Check just a portion of the actual request as the end will be different with each encoding
        assertEquals(expectedRequestBody, actualRequestBody.take(expectedRequestBody.length))
    }

    @Test
    fun `flattened private key can be used`() {
        // Set up mock web service
        val responseJson = """
            |{
            |  "access_token": "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y_-zBvt2kw3xn-MY3gY4lOXPee6iKPw3JncYBT1Y-kdPpBYl-lsmUlA4x5dUVC1qbjEi1OHfe_Oa-VRUAeabnMLjYgKI7b",
            |  "token_type": "bearer",
            |  "expires_in": 3600,
            |  "scope": "Patient.read Patient.search"
            |}
        """.trimMargin()

        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))
        mockWebServer.start()

        val flattenedPrivateKey = this::class.java.getResource("/FlattenedTestPrivateKey.txt")!!.readText()
        val tenant =
            createTestTenant(
                clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                authEndpoint = "${mockWebServer.url("/interconnect-aocurprd-oauth")}/oauth2/token",
                privateKey = flattenedPrivateKey,
                tenantMnemonic = "TestTenant"
            )

        // Execute test
        val authentication = authenticationService.getAuthentication(tenant)!!

        // Validate Response
        assertEquals("Patient.read Patient.search", authentication.scope)
        assertNotNull(authentication.expiresAt)
        assertEquals("bearer", authentication.tokenType)
        assertEquals(
            "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y_-zBvt2kw3xn-MY3gY4lOXPee6iKPw3JncYBT1Y-kdPpBYl-lsmUlA4x5dUVC1qbjEi1OHfe_Oa-VRUAeabnMLjYgKI7b",
            authentication.accessToken
        )

        // Validate Request
        val expectedRequestBody = "grant_type=client_credentials&" +
            "client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer&" +
            "client_assertion="
        val actualRequest = mockWebServer.takeRequest()
        assertEquals(
            "application/x-www-form-urlencoded; charset=UTF-8",
            actualRequest.getHeader("Content-Type")
        )
        assertEquals("application/json", actualRequest.getHeader("Accept"))
        val actualRequestBody = actualRequest.body.readUtf8()
        // Check just a portion of the actual request as the end will be different with each encoding
        assertEquals(expectedRequestBody, actualRequestBody.take(expectedRequestBody.length))
    }

    @Test
    fun `hsi value properly included`() {
        // Set up mock web service
        val responseJson = """
            |{
            |  "access_token": "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y_-zBvt2kw3xn-MY3gY4lOXPee6iKPw3JncYBT1Y-kdPpBYl-lsmUlA4x5dUVC1qbjEi1OHfe_Oa-VRUAeabnMLjYgKI7b",
            |  "token_type": "bearer",
            |  "expires_in": 3600,
            |  "scope": "Patient.read Patient.search"
            |}
        """.trimMargin()

        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))
        mockWebServer.start()

        val flattenedPrivateKey = this::class.java.getResource("/FlattenedTestPrivateKey.txt")!!.readText()
        val tenant =
            createTestTenant(
                clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                authEndpoint = "${mockWebServer.url("/interconnect-aocurprd-oauth")}/oauth2/token",
                privateKey = flattenedPrivateKey,
                tenantMnemonic = "TestTenant",
                hsi = "HSI:Value"
            )

        // Execute test
        val authentication = authenticationService.getAuthentication(tenant)!!

        // Validate Response
        assertEquals("Patient.read Patient.search", authentication.scope)
        assertNotNull(authentication.expiresAt)
        assertEquals("bearer", authentication.tokenType)
        assertEquals(
            "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y_-zBvt2kw3xn-MY3gY4lOXPee6iKPw3JncYBT1Y-kdPpBYl-lsmUlA4x5dUVC1qbjEi1OHfe_Oa-VRUAeabnMLjYgKI7b",
            authentication.accessToken
        )

        // Validate Request
        val expectedRequestBody = "grant_type=client_credentials&" +
            "client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer&" +
            "client_assertion="
        val actualRequest = mockWebServer.takeRequest()
        assertEquals(
            "application/x-www-form-urlencoded; charset=UTF-8",
            actualRequest.getHeader("Content-Type")
        )
        assertEquals("application/json", actualRequest.getHeader("Accept"))
        val actualRequestBody = actualRequest.body.readUtf8()
        // Check just a portion of the actual request as the end will be different with each encoding
        assertEquals(expectedRequestBody, actualRequestBody.take(expectedRequestBody.length))
    }
}
