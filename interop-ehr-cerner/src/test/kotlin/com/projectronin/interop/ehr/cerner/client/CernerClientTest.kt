package com.projectronin.interop.ehr.cerner.client

import com.projectronin.interop.common.http.FhirJson
import com.projectronin.interop.common.http.exceptions.RequestFailureException
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.cerner.auth.CernerAuthentication
import com.projectronin.interop.ehr.cerner.createTestTenant
import com.projectronin.interop.ehr.cerner.getClient
import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.fhir.r4.resource.Communication
import com.projectronin.interop.fhir.r4.valueset.EventStatus
import com.projectronin.interop.fhir.util.asCode
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
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

class CernerClientTest {
    private val patientResult = this::class.java.getResource("/ExamplePatientResponse.json")!!.readText()
    private val authenticationBroker = mockk<EHRAuthenticationBroker>()
    private val datalakePublishService =
        mockk<DatalakePublishService> {
            every { publishRawData(any(), any(), any()) } returns "12345"
        }
    private val cernerClient = CernerClient(getClient(), authenticationBroker, datalakePublishService)
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
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                serviceEndpoint = mockWebServer.url("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d").toString(),
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )
        every { runBlocking { authenticationBroker.getAuthentication(tenant) } } returns null

        assertThrows<IllegalStateException> {
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
            MockResponse().setBody(patientResult).setHeader("Content-Type", "application/json"),
        )
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                serviceEndpoint = mockWebServer.url("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d").toString(),
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )

        val authentication = CernerAuthentication("accessToken", "Bearer", 570, "system/Patient.read", "")
        every { runBlocking { authenticationBroker.getAuthentication(tenant) } } returns authentication

        val response =
            runBlocking {
                val httpResponse =
                    cernerClient.get(
                        tenant,
                        "/Patient/12724066",
                    )
                httpResponse.httpResponse.bodyAsText()
            }
        assertEquals(patientResult, response)
        val requestUrl = mockWebServer.takeRequest().path
        assertTrue(requestUrl?.contains("/Patient/12724066") == true)
    }

    @Test
    fun `ensure get operation returns correctly with parameters`() {
        mockWebServer.enqueue(
            MockResponse().setBody(patientResult).setHeader("Content-Type", "application/json"),
        )
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                serviceEndpoint = mockWebServer.url("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d").toString(),
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )

        val authentication = CernerAuthentication("accessToken", "Bearer", 570, "system/Patient.read", "")
        every { runBlocking { authenticationBroker.getAuthentication(tenant) } } returns authentication

        val response =
            runBlocking {
                val httpResponse =
                    cernerClient.get(
                        tenant,
                        "/Patient/12724066",
                        parameters =
                            mapOf(
                                "simple" to "simple",
                                "single" to listOf("1", "b", "special="),
                                "repeating" to RepeatingParameter(listOf("first", "second")),
                                "noValue" to null,
                            ),
                    )
                httpResponse.httpResponse.bodyAsText()
            }
        assertEquals(patientResult, response)
        val requestUrl = mockWebServer.takeRequest().path
        assertEquals(true, requestUrl?.contains("/Patient/12724066"))
        assertEquals(true, requestUrl?.contains("simple=simple"))
        assertEquals(true, requestUrl?.contains("single=1,b,special%3D"))
        assertEquals(true, requestUrl?.contains("repeating=first"))
        assertEquals(true, requestUrl?.contains("repeating=second"))
        assertEquals(false, requestUrl?.contains("noValue"))
    }

    @Test
    fun `ensure get operation returns correctly with no timeout parameters`() {
        mockWebServer.enqueue(
            MockResponse()
                .setBody(patientResult)
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(4, TimeUnit.SECONDS),
        )
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                serviceEndpoint = mockWebServer.url("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d").toString(),
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )

        val authentication = CernerAuthentication("accessToken", "Bearer", 570, "system/Patient.read", "")
        every { runBlocking { authenticationBroker.getAuthentication(tenant) } } returns authentication

        val response =
            runBlocking {
                val httpResponse =
                    cernerClient.get(
                        tenant,
                        "/Patient/12724066",
                    )
                httpResponse.httpResponse.bodyAsText()
            }
        assertEquals(patientResult, response)
        val requestUrl = mockWebServer.takeRequest().path
        assertTrue(requestUrl?.contains("/Patient/12724066") == true)
    }

    @Test
    fun `ensure get operation times out`() {
        mockWebServer.enqueue(
            MockResponse()
                .setBody(patientResult)
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(4, TimeUnit.SECONDS),
        )
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                serviceEndpoint = mockWebServer.url("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d").toString(),
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )

        val authentication = CernerAuthentication("accessToken", "Bearer", 570, "system/Patient.read", "")
        every { runBlocking { authenticationBroker.getAuthentication(tenant) } } returns authentication

        assertThrows<RequestFailureException> {
            runBlocking {
                cernerClient.get(
                    tenant,
                    "/Patient/12724066",
                    timeoutOverride = 1.seconds,
                )
            }
        }
    }

    @Test
    fun `throws exception when unable to retrieve authentication during a post operation`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                serviceEndpoint = mockWebServer.url("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d").toString(),
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )
        every { runBlocking { authenticationBroker.getAuthentication(tenant) } } returns null

        val communication =
            Communication(
                status = EventStatus.COMPLETED.asCode(),
            )

        assertThrows<IllegalStateException> {
            runBlocking {
                cernerClient.post(tenant, "/Communication", communication)
            }
        }
    }

    @Test
    fun `ensure post operation submits to server correctly`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(HttpStatusCode.Created.value),
        )

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                serviceEndpoint = mockWebServer.url("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d").toString(),
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )
        val authentication = CernerAuthentication("accessToken", "Bearer", 570, "system/Location.write", "")
        every { runBlocking { authenticationBroker.getAuthentication(tenant) } } returns authentication

        val communication =
            Communication(
                status = EventStatus.COMPLETED.asCode(),
            )

        val response =
            runBlocking {
                cernerClient.post(tenant, "/Communication", communication)
            }
        assertEquals(HttpStatusCode.Created, response.httpResponse.status)

        val request = mockWebServer.takeRequest()

        assertEquals("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d/Communication", request.path)
        assertEquals("Bearer accessToken", request.headers["Authorization"])
        assertTrue(request.headers["Accept"]!!.contains("application/fhir+json"))
        assertTrue(request.headers["Content-Type"]!!.startsWith("application/fhir+json"))
        assertEquals("""{"resourceType":"Communication","status":"completed"}""", request.body.readUtf8())
    }

    @Test
    fun `ensure get operation accept header is correct when changing accept type override`() {
        mockWebServer.enqueue(
            MockResponse().setBody(patientResult).setHeader("Content-Type", "application/json"),
        )
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                serviceEndpoint = mockWebServer.url("/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d").toString(),
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )

        val authentication = CernerAuthentication("accessToken", "Bearer", 570, "system/Patient.read", "")
        every { runBlocking { authenticationBroker.getAuthentication(tenant) } } returns authentication

        val response =
            runBlocking {
                cernerClient.get(
                    tenant,
                    "/Patient/12724066",
                    parameters =
                        mapOf(
                            "simple" to "simple",
                            "single" to listOf("1", "b", "special="),
                            "repeating" to RepeatingParameter(listOf("first", "second")),
                            "noValue" to null,
                        ),
                    acceptTypeOverride = ContentType.Application.FhirJson,
                )
            }
        assertEquals(HttpStatusCode.OK, response.httpResponse.status)

        val request = mockWebServer.takeRequest()

        assertTrue(request.headers["Accept"]!!.contains("application/fhir+json"))
        assertTrue(request.headers["Content-Type"]!!.startsWith("application/fhir+json"))
    }
}
