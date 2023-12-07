package com.projectronin.interop.ehr.cerner.auth

import com.projectronin.interop.common.http.NO_RETRY_HEADER
import com.projectronin.interop.common.http.exceptions.RequestFailureException
import com.projectronin.interop.common.http.exceptions.ServerFailureException
import com.projectronin.interop.ehr.cerner.CernerFHIRService
import com.projectronin.interop.ehr.cerner.createTestTenant
import com.projectronin.interop.ehr.cerner.getClient
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext
import java.net.URLDecoder

class CernerAuthenticationServiceTest {
    private val applicationContext = mockk<ApplicationContext>()
    private val authenticationService = CernerAuthenticationService(getClient(), applicationContext)
    private lateinit var mockWebServer: MockWebServer
    private lateinit var tenant: Tenant

    class FakeResource : Resource<FakeResource> {
        override val resourceType = "Fake"
        override val id: Nothing? = null
        override var meta: Meta? = null
        override val implicitRules: Nothing? = null
        override val language: Nothing? = null
    }

    // Has default read scope
    class ReadService : CernerFHIRService<FakeResource>(mockk()) {
        override val fhirURLSearchPart = "/Read"
        override val fhirResourceType = FakeResource::class.java
    }

    // Has write scope
    class WriteService : CernerFHIRService<FakeResource>(mockk()) {
        override val fhirURLSearchPart = "/Write"
        override val fhirResourceType = FakeResource::class.java
        override val readScope = false
        override val writeScope = true
    }

    // Has read and write scope
    class ReadAndWriteService : CernerFHIRService<FakeResource>(mockk()) {
        override val fhirURLSearchPart = "/ReadAndWrite"
        override val fhirResourceType = FakeResource::class.java
        override val writeScope = true
    }

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "${mockWebServer.url("/protocols")}/oauth2/profiles/smart-v1/token",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `can read auth request from server`() {
        val responseJson =
            """
            |{
            |    "access_token": "accesstoken",
            |    "scope": "system/Read.read",
            |    "token_type": "Bearer",
            |    "expires_in": 570
            |}
            """.trimMargin()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))

        every { applicationContext.getBeansOfType<CernerFHIRService<*>>() } returns mapOf("CernerReadService" to ReadService())

        val authResponse = runBlocking { authenticationService.getAuthentication(tenant) }

        assertEquals("system/Read.read", authResponse?.scope)
        assertNotNull(authResponse?.expiresAt)
        assertEquals("Bearer", authResponse?.tokenType)
        assertEquals(
            "accesstoken",
            authResponse?.accessToken,
        )
    }

    @Test
    fun `handles exception from auth request`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        every { applicationContext.getBeansOfType<CernerFHIRService<*>>() } returns mapOf("CernerReadService" to ReadService())

        assertThrows<ServerFailureException> {
            runBlocking { authenticationService.getAuthentication(tenant) }
        }
    }

    @Test
    fun `handles no auth services found`() {
        every { applicationContext.getBeansOfType<CernerFHIRService<*>>() } returns mapOf()

        assertThrows<RequestFailureException> {
            runBlocking { authenticationService.getAuthentication(tenant) }
        }
    }

    @Test
    fun `sends read scope`() {
        val responseJson =
            """
            |{
            |    "access_token": "accesstoken",
            |    "scope": "system/Read.read",
            |    "token_type": "Bearer",
            |    "expires_in": 570
            |}}
            """.trimMargin()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))

        every { applicationContext.getBeansOfType<CernerFHIRService<*>>() } returns mapOf("CernerReadService" to ReadService())

        runBlocking { authenticationService.getAuthentication(tenant) }
        val recordedRequest = mockWebServer.takeRequest()
        val scope = getScopeFromRequest(recordedRequest)

        assertEquals("system/Read.read", scope)
    }

    @Test
    fun `sends write scope`() {
        val responseJson =
            """
            |{
            |    "access_token": "accesstoken",
            |    "scope": "system/Write.write",
            |    "token_type": "Bearer",
            |    "expires_in": 570
            |}}
            """.trimMargin()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))

        every { applicationContext.getBeansOfType<CernerFHIRService<*>>() } returns mapOf("CernerWriteService" to WriteService())

        runBlocking { authenticationService.getAuthentication(tenant) }
        val recordedRequest = mockWebServer.takeRequest()
        val scope = getScopeFromRequest(recordedRequest)

        assertEquals("system/Write.write", scope)
    }

    @Test
    fun `sends combination of scopes`() {
        val responseJson =
            """
            |{
            |    "access_token": "accesstoken",
            |    "scope": "system/Write.write system/ReadAndWrite.read system/ReadAndWrite.write",
            |    "token_type": "Bearer",
            |    "expires_in": 570
            |}}
            """.trimMargin()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))

        every { applicationContext.getBeansOfType<CernerFHIRService<*>>() } returns
            mapOf(
                "CernerWriteService" to WriteService(),
                "CernerReadAndWriteService" to ReadAndWriteService(),
            )

        runBlocking { authenticationService.getAuthentication(tenant) }
        val recordedRequest = mockWebServer.takeRequest()
        val scope = getScopeFromRequest(recordedRequest)!!

        assertTrue(scope.contains("system/Write.write"))
        assertTrue(scope.contains("system/ReadAndWrite.read"))
        assertTrue(scope.contains("system/ReadAndWrite.write"))
    }

    @Test
    fun `sends retry header when true`() {
        val responseJson =
            """
            |{
            |    "access_token": "accesstoken",
            |    "scope": "system/Read.read",
            |    "token_type": "Bearer",
            |    "expires_in": 570
            |}}
            """.trimMargin()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))

        every { applicationContext.getBeansOfType<CernerFHIRService<*>>() } returns mapOf("CernerReadService" to ReadService())

        runBlocking { authenticationService.getAuthentication(tenant, true) }
        val recordedRequest = mockWebServer.takeRequest()

        assertEquals("true", recordedRequest.headers.get(NO_RETRY_HEADER))
    }

    @Test
    fun `sends retry header when false`() {
        val responseJson =
            """
            |{
            |    "access_token": "accesstoken",
            |    "scope": "system/Read.read",
            |    "token_type": "Bearer",
            |    "expires_in": 570
            |}}
            """.trimMargin()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))

        every { applicationContext.getBeansOfType<CernerFHIRService<*>>() } returns mapOf("CernerReadService" to ReadService())

        runBlocking { authenticationService.getAuthentication(tenant, false) }
        val recordedRequest = mockWebServer.takeRequest()

        assertEquals("false", recordedRequest.headers.get(NO_RETRY_HEADER))
    }

    @Test
    fun `sends retry header when not provided`() {
        val responseJson =
            """
            |{
            |    "access_token": "accesstoken",
            |    "scope": "system/Read.read",
            |    "token_type": "Bearer",
            |    "expires_in": 570
            |}}
            """.trimMargin()
        mockWebServer.enqueue(MockResponse().setBody(responseJson).setHeader("Content-Type", "application/json"))

        every { applicationContext.getBeansOfType<CernerFHIRService<*>>() } returns mapOf("CernerReadService" to ReadService())

        runBlocking { authenticationService.getAuthentication(tenant) }
        val recordedRequest = mockWebServer.takeRequest()

        assertEquals("false", recordedRequest.headers.get(NO_RETRY_HEADER))
    }

    private fun getScopeFromRequest(request: RecordedRequest): String? {
        // There's probably a built in function to do this, but I couldn't find it
        val requestBody = URLDecoder.decode(request.body.readUtf8(), "UTF-8")
        val parameters =
            requestBody.split("&").associate {
                val paramParts = it.split("=")
                Pair(paramParts[0], paramParts[1])
            }
        return parameters["scope"]
    }
}
