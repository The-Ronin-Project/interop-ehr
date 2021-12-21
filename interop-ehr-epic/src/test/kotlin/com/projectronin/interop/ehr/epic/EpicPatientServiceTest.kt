package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicPatientBundle
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

class EpicPatientServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var httpResponse: HttpResponse
    private val validPatientSearchJson = this::class.java.getResource("/ExamplePatientBundle.json")!!.readText()
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()

    @BeforeEach
    fun initTest() {
        epicClient = mockk<EpicClient>()
        httpResponse = mockk<HttpResponse>()
    }

    @Test
    fun `ensure patient is returned`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<String>() } returns validPatientSearchJson
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("given" to "givenName", "family" to "familyName", "birthdate" to "birthDate")
            )
        } returns httpResponse

        val bundle =
            EpicPatientService(epicClient).findPatient(
                tenant,
                "birthDate",
                "givenName",
                "familyName"
            )
        assertEquals(EpicPatientBundle(validPatientSearchJson), bundle)
    }

    @Test
    fun `ensure http error handled`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.NotFound
        coEvery { httpResponse.receive<String>() } returns validPatientSearchJson
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("given" to "givenName", "family" to "familyName", "birthdate" to "birthDate")
            )
        } returns httpResponse

        assertThrows<IOException> {
            EpicPatientService(epicClient).findPatient(
                tenant,
                "birthDate",
                "givenName",
                "familyName"
            )
        }
    }
}
