package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.util.reflect.TypeInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CernerFHIRServiceTest {
    private lateinit var cernerClient: CernerClient
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private val patientBundle = readResource<Bundle>("/ExamplePatientBundle.json")

    @BeforeEach
    fun setup() {
        cernerClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
    }

    private class TestService(
        cernerClient: CernerClient,
        override val fhirURLSearchPart: String = "url",
        override val fhirResourceType: Class<Patient> = Patient::class.java
    ) :
        CernerFHIRService<Patient>(cernerClient) {
        fun getPatients(tenant: Tenant, overrideParameters: Map<String, Any?> = emptyMap()): List<Patient> {
            return getResourceListFromSearch(tenant, overrideParameters)
        }
    }

    @Test
    fun `getById works`() {
        val tenant = createTestTenant(
            clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
            authEndpoint = "https://example.org",
            secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
        )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Patient>(TypeInfo(Patient::class, Patient::class.java)) } returns mockk(relaxed = true) {
            every { id } returns mockk { every { value } returns "difId" }
        }
        coEvery {
            cernerClient.get(
                tenant,
                "url/fhirId"
            )
        } returns ehrResponse

        val service = TestService(cernerClient)
        val example = service.getByID(tenant, "fhirId")
        assertEquals("difId", example.id?.value)
    }

    @Test
    fun `ensure standard parameters are added when missing`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns patientBundle
        coEvery {
            cernerClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 250
                )
            )
        } returns ehrResponse

        val service = TestService(cernerClient)
        val patients = service.getPatients(tenant, mapOf("_count" to 250))
        assertEquals(2, patients.size)
    }

    @Test
    fun `ensure standard parameters are not included when already provided`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns patientBundle
        coEvery {
            cernerClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 20
                )
            )
        } returns ehrResponse

        val service = TestService(cernerClient)
        val patients = service.getPatients(tenant)
        assertEquals(2, patients.size)
    }

    @Test
    fun `ensure bundle handles next link with URL`() {
        val tenant = createTestTenant(
            clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
            authEndpoint = "https://example.org",
            secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
        )

        val patient1 = mockk<Patient>(relaxed = true) {
            every { id } returns Id("1234")
        }
        val patient2 = mockk<Patient>(relaxed = true) {
            every { id } returns Id("5678")
        }
        val bundle1 = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf(
                mockk {
                    every { relation } returns FHIRString("next")
                    every { url } returns Uri("http://test/1234")
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns patient1
                }
            )
        }
        val bundle2 = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf()
            every { entry } returns listOf(
                mockk {
                    every { resource } returns patient2
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle1
        coEvery {
            cernerClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 20
                )
            )
        } returns ehrResponse

        val httpResponse2 = mockk<HttpResponse> {
            every { status } returns HttpStatusCode.OK
            coEvery { body<Bundle>() } returns bundle2
        }
        val ehrResponse2 = EHRResponse(httpResponse2, "67890")
        coEvery { cernerClient.get(tenant, "http://test/1234") } returns ehrResponse2

        val service = TestService(cernerClient)
        val patients = service.getPatients(tenant)
        assertEquals(2, patients.size)
    }

    @Test
    fun `ensure bundle handles no links`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        val patient = mockk<Patient>(relaxed = true)
        val bundle = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf()
            every { entry } returns listOf(
                mockk {
                    every { resource } returns patient
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            cernerClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 20
                )
            )
        } returns ehrResponse

        val service = TestService(cernerClient)
        val patients = service.getPatients(tenant)
        assertEquals(1, patients.size)
    }

    @Test
    fun `ensure bundle handles no next links`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        val patient = mockk<Patient>(relaxed = true)
        val bundle = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf(
                mockk {
                    every { relation } returns FHIRString("self")
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns patient
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            cernerClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 20
                )
            )
        } returns ehrResponse

        val service = TestService(cernerClient)
        val patients = service.getPatients(tenant)
        assertEquals(1, patients.size)
    }

    @Test
    fun `ensure bundle handles links with no relations`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        val patient = mockk<Patient>(relaxed = true)
        val bundle = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf(
                mockk {
                    every { relation } returns null
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns patient
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            cernerClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 20
                )
            )
        } returns ehrResponse

        val service = TestService(cernerClient)
        val patients = service.getPatients(tenant)
        assertEquals(1, patients.size)
    }

    @Test
    fun `ensure bundle handles next link with no URL`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        val patient = mockk<Patient>(relaxed = true)
        val bundle = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf(
                mockk {
                    every { relation } returns FHIRString("next")
                    every { url } returns null
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns patient
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            cernerClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 20
                )
            )
        } returns ehrResponse

        val service = TestService(cernerClient)
        val patients = service.getPatients(tenant)
        assertEquals(1, patients.size)
    }

    @Test
    fun `ensure bundle handles next link with URL with no value`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
            )

        val patient = mockk<Patient>(relaxed = true)
        val bundle = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf(
                mockk {
                    every { relation } returns FHIRString("next")
                    every { url } returns Uri(null)
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns patient
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            cernerClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 20
                )
            )
        } returns ehrResponse

        val service = TestService(cernerClient)
        val patients = service.getPatients(tenant)
        assertEquals(1, patients.size)
    }
}
