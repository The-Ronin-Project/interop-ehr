package com.projectronin.interop.ehr.client

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.auth.BrokeredAuthenticator
import com.projectronin.interop.common.http.exceptions.RequestFailureException
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class EHRClientTest {
    private val authenticationBroker = mockk<EHRAuthenticationBroker>()
    private val datalakePublishService =
        mockk<DatalakePublishService> {
            every { publishRawData(any(), any(), any()) } returns "12345"
        }
    private val httpClient =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                jackson {
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                }
            }
            install(HttpTimeout)
        }
    private val client = TestEHRClient(httpClient, authenticationBroker, datalakePublishService)

    private lateinit var mockWebServer: MockWebServer

    private val basicJson = "{\"success\":true}"
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "tenant"
            every { name } returns "Test Tenant"
        }

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val authentication =
            mockk<Authentication> {
                every { accessToken } returns "access-token"
            }
        val authenticator =
            mockk<BrokeredAuthenticator> {
                every { getAuthentication(any()) } returns authentication
            }
        every { authenticationBroker.getAuthenticator(tenant) } returns authenticator

        every { tenant.vendor.serviceEndpoint } returns mockWebServer.url("").toString()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `get works with no parameters`() {
        mockWebServer.enqueue(
            MockResponse().setBody(basicJson).setHeader("Content-Type", "application/json"),
        )

        val authentication =
            mockk<Authentication> {
                every { accessToken } returns "access-token"
            }
        val authenticator =
            mockk<BrokeredAuthenticator> {
                every { getAuthentication(any()) } returns authentication
            }
        every { runBlocking { authenticationBroker.getAuthenticator(tenant) } } returns authenticator

        val response =
            runBlocking {
                val httpResponse =
                    client.get(
                        tenant,
                        "/Patient/12724066",
                    )
                httpResponse.httpResponse.bodyAsText()
            }
        assertEquals(basicJson, response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("/Patient/12724066") == true)
    }

    @Test
    fun `get performs retry on 401`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(401),
        )
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(basicJson).setHeader("Content-Type", "application/json"),
        )

        val authentication =
            mockk<Authentication> {
                every { accessToken } returns "access-token"
            }
        val authenticator =
            mockk<BrokeredAuthenticator> {
                every { getAuthentication(any()) } returns authentication
            }
        every { runBlocking { authenticationBroker.getAuthenticator(tenant) } } returns authenticator

        val response =
            runBlocking {
                val httpResponse =
                    client.get(
                        tenant,
                        "/Patient/12724066",
                    )
                httpResponse.httpResponse.bodyAsText()
            }
        assertEquals(basicJson, response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("/Patient/12724066") == true)

        verifySequence {
            authenticator.getAuthentication(false) // initial getAuth request
            authenticator.getAuthentication(true) // retry getAuth request
        }
    }

    @Test
    fun `get works with string parameter`() {
        mockWebServer.enqueue(
            MockResponse().setBody(basicJson).setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                val httpResponse =
                    client.get(
                        tenant,
                        "/Patient",
                        mapOf("_id" to "123456"),
                    )
                httpResponse.httpResponse.bodyAsText()
            }
        assertEquals(basicJson, response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("/Patient?_id=123456") == true)
    }

    @Test
    fun `get works with list parameter`() {
        mockWebServer.enqueue(
            MockResponse().setBody(basicJson).setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                val httpResponse =
                    client.get(
                        tenant,
                        "/Patient",
                        mapOf("_id" to listOf("12345", "67890")),
                    )
                httpResponse.httpResponse.bodyAsText()
            }
        assertEquals(basicJson, response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("/Patient?_id=12345,67890") == true)
    }

    @Test
    fun `get works with repeating parameter`() {
        mockWebServer.enqueue(
            MockResponse().setBody(basicJson).setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                val httpResponse =
                    client.get(
                        tenant,
                        "/Patient",
                        mapOf("_id" to RepeatingParameter(listOf("12345", "67890"))),
                    )
                httpResponse.httpResponse.bodyAsText()
            }
        assertEquals(basicJson, response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("/Patient?_id=12345&_id=67890") == true)
    }

    @Test
    fun `get times out with timeout parameters`() {
        mockWebServer.enqueue(
            MockResponse()
                .setBody(basicJson)
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(4, TimeUnit.SECONDS),
        )

        assertThrows<RequestFailureException> {
            runBlocking {
                client.get(
                    tenant,
                    "/Patient/12724066",
                    timeoutOverride = 1.seconds,
                )
            }
        }
    }

    @Test
    fun `get works with no timeout parameters`() {
        mockWebServer.enqueue(
            MockResponse()
                .setBody(basicJson)
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(4, TimeUnit.SECONDS),
        )

        val response =
            runBlocking {
                val httpResponse =
                    client.get(
                        tenant,
                        "/Patient/12724066",
                    )
                httpResponse.httpResponse.bodyAsText()
            }
        assertEquals(basicJson, response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("/Patient/12724066") == true)
    }

    @Test
    fun `post works with no parameters`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(HttpStatusCode.Created.value),
        )

        val response =
            runBlocking {
                client.post(
                    tenant,
                    "/Patient/12724066",
                    basicJson,
                )
            }
        assertEquals(HttpStatusCode.Created, response.httpResponse.status)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path?.contains("/Patient/12724066") == true)
        assertEquals("Bearer access-token", request.headers["Authorization"])
        assertTrue(request.headers["Accept"]!!.contains("application/json"))
        assertTrue(request.headers["Content-Type"]!!.startsWith("application/json"))
        assertEquals(basicJson, request.body.readUtf8())
    }

    @Test
    fun `post works with string parameter`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(HttpStatusCode.Created.value),
        )

        val response =
            runBlocking {
                client.post(
                    tenant,
                    "/Patient",
                    basicJson,
                    mapOf("_id" to "12345"),
                )
            }
        assertEquals(HttpStatusCode.Created, response.httpResponse.status)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path?.contains("/Patient?_id=12345") == true)
        assertEquals("Bearer access-token", request.headers["Authorization"])
        assertTrue(request.headers["Accept"]!!.contains("application/json"))
        assertTrue(request.headers["Content-Type"]!!.startsWith("application/json"))
        assertEquals(basicJson, request.body.readUtf8())
    }

    @Test
    fun `post works with list parameter`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(HttpStatusCode.Created.value),
        )

        val response =
            runBlocking {
                client.post(
                    tenant,
                    "/Patient",
                    basicJson,
                    mapOf("_id" to listOf("12345", "67890")),
                )
            }
        assertEquals(HttpStatusCode.Created, response.httpResponse.status)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path?.contains("/Patient?_id=12345,67890") == true)
        assertEquals("Bearer access-token", request.headers["Authorization"])
        assertTrue(request.headers["Accept"]!!.contains("application/json"))
        assertTrue(request.headers["Content-Type"]!!.startsWith("application/json"))
        assertEquals(basicJson, request.body.readUtf8())
    }

    @Test
    fun `post works with repeating parameter`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(HttpStatusCode.Created.value),
        )

        val response =
            runBlocking {
                client.post(
                    tenant,
                    "/Patient",
                    basicJson,
                    mapOf("_id" to RepeatingParameter(listOf("12345", "67890"))),
                )
            }
        assertEquals(HttpStatusCode.Created, response.httpResponse.status)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path?.contains("/Patient?_id=12345&_id=67890") == true)
        assertEquals("Bearer access-token", request.headers["Authorization"])
        assertTrue(request.headers["Accept"]!!.contains("application/json"))
        assertTrue(request.headers["Content-Type"]!!.startsWith("application/json"))
        assertEquals(basicJson, request.body.readUtf8())
    }

    @Test
    fun `put performs retry on 401`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(401),
        )
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(basicJson).setHeader("Content-Type", "application/json"),
        )

        val authentication =
            mockk<Authentication> {
                every { accessToken } returns "access-token"
            }
        val authenticator =
            mockk<BrokeredAuthenticator> {
                every { getAuthentication(any()) } returns authentication
            }
        every { runBlocking { authenticationBroker.getAuthenticator(tenant) } } returns authenticator

        val response =
            runBlocking {
                val httpResponse =
                    client.put(
                        tenant,
                        "/Patient/12724066",
                        basicJson,
                    )
                httpResponse.httpResponse.bodyAsText()
            }
        assertEquals(basicJson, response)
        val request = mockWebServer.takeRequest()
        assertEquals("PUT", request.method)
        assertTrue(request.path?.contains("/Patient/12724066") == true)
        assertEquals(basicJson, request.body.readUtf8())
        assertEquals("Bearer access-token", request.headers["Authorization"])
        assertTrue(request.headers["Accept"]!!.contains("application/json"))
        assertTrue(request.headers["Content-Type"]!!.startsWith("application/json"))

        verifySequence {
            authenticator.getAuthentication(false) // initial getAuth request
            authenticator.getAuthentication(true) // retry getAuth request
        }
    }

    @Test
    fun `options performs retry on 401`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(401),
        )
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(basicJson).setHeader("Content-Type", "application/json"),
        )

        val authentication =
            mockk<Authentication> {
                every { accessToken } returns "access-token"
            }
        val authenticator =
            mockk<BrokeredAuthenticator> {
                every { getAuthentication(any()) } returns authentication
            }
        every { runBlocking { authenticationBroker.getAuthenticator(tenant) } } returns authenticator

        val response =
            runBlocking {
                val httpResponse =
                    client.options(
                        tenant,
                        "/Patient/12724066",
                    )
                httpResponse.bodyAsText()
            }
        assertEquals(basicJson, response)
        val request = mockWebServer.takeRequest()
        assertEquals("OPTIONS", request.method)
        assertTrue(request.path?.contains("/Patient/12724066") == true)
        assertEquals("Bearer access-token", request.headers["Authorization"])

        verifySequence {
            authenticator.getAuthentication(false) // initial getAuth request
            authenticator.getAuthentication(true) // retry getAuth request
        }
    }

    private class TestEHRClient(
        client: HttpClient,
        authenticationBroker: EHRAuthenticationBroker,
        datalakeService: DatalakePublishService,
    ) : EHRClient(client, authenticationBroker, datalakeService)
}
