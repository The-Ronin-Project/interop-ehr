package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
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

    @BeforeEach
    fun setup() {
        cernerClient = mockk()
        httpResponse = mockk()
    }
    private class TestService(
        cernerClient: CernerClient,
        override val fhirURLSearchPart: String = "url",
        override val fhirResourceType: Class<Patient> = Patient::class.java
    ) :
        CernerFHIRService<Patient>(cernerClient) {
        fun getPatients(tenant: Tenant): List<Patient> {
            return getResourceListFromSearch(tenant, emptyMap())
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
        coEvery { httpResponse.body<Patient>(TypeInfo(Patient::class, Patient::class.java)) } returns mockk {
            every { id } returns mockk { every { value } returns "difId" }
        }
        coEvery {
            cernerClient.get(
                tenant,
                "url/fhirId",
            )
        } returns httpResponse

        val service = TestService(cernerClient)
        val example = service.getByID(tenant, "fhirId")
        assertEquals("difId", example.id?.value)
    }

    @Test
    fun `ensure bundle handles no links`() {
        val tenant = createTestTenant(
            clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
            authEndpoint = "https://example.org",
            secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
        )

        val patient = mockk<Patient>()
        val bundle = mockk<Bundle> {
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
                emptyMap()
            )
        } returns httpResponse

        val service = TestService(cernerClient)
        val patients = service.getPatients(tenant)
        assertEquals(1, patients.size)
    }

    @Test
    fun `ensure bundle handles next link with URL`() {
        val tenant = createTestTenant(
            clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
            authEndpoint = "https://example.org",
            secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
        )

        val patient = mockk<Patient>()
        val bundle1 = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf(
                mockk {
                    every { relation } returns FHIRString("next")
                    every { url } returns Uri("http://test/1234")
                }
            )
            every { entry } returns listOf(
                mockk {
                    every { resource } returns patient
                }
            )
        }
        val bundle2 = mockk<Bundle>(relaxed = true) {
            every { link } returns listOf()
            every { entry } returns listOf(
                mockk {
                    every { resource } returns patient
                }
            )
        }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle1
        coEvery {
            cernerClient.get(
                tenant,
                "url"
            )
        } returns httpResponse

        val httpResponse2 = mockk<HttpResponse> {
            every { status } returns HttpStatusCode.OK
            coEvery { body<Bundle>() } returns bundle2
        }

        coEvery { cernerClient.get(tenant, "http://test/1234") } returns httpResponse2

        val service = TestService(cernerClient)
        val conditions = service.getPatients(tenant)
        assertEquals(2, conditions.size)
    }
}
