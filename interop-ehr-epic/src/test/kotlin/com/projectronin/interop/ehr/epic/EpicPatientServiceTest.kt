package com.projectronin.interop.ehr.epic

import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Patient
import io.ktor.client.call.body
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
import java.time.LocalDate
import com.projectronin.interop.aidbox.PatientService as AidboxPatientService

class EpicPatientServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var aidboxClient: AidboxPatientService
    private lateinit var httpResponse: HttpResponse
    private val validPatientBundle = readResource<Bundle>("/ExamplePatientBundle.json")
    private val validMultiPatientBundle = readResource<Bundle>("/ExampleMultiPatientBundle.json")
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()

    @BeforeEach
    fun initTest() {
        epicClient = mockk()
        aidboxClient = mockk()
        httpResponse = mockk()
    }

    @Test
    fun `ensure, find patient, patient is returned`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225", "https://example.org", testPrivateKey, "TEST_TENANT"
        )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("given" to "givenName", "family" to "familyName", "birthdate" to "2015-01-01")
            )
        } returns httpResponse

        val bundle = EpicPatientService(epicClient, 100, aidboxClient).findPatient(
            tenant, LocalDate.of(2015, 1, 1), "givenName", "familyName"
        )
        assertEquals(validPatientBundle.entry.map { it.resource }.filterIsInstance<Patient>(), bundle)
    }

    @Test
    fun `ensure, find patient by identifier, returns single patient`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"
        )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|202497")
            )
        } returns httpResponse

        val resultPatientsByKey = EpicPatientService(epicClient, 100, aidboxClient).findPatientsById(
            tenant,
            mapOf(
                "patient#1" to Identifier(
                    value = "202497",
                    system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                    type = CodeableConcept(text = "MRN")
                )
            )
        )
        assertEquals(mapOf("patient#1" to validPatientBundle.entry.first().resource), resultPatientsByKey)
    }

    @Test
    fun `ensure, find patient by identifier, single patient matches multiple requests, each returned the patient`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"
        )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|202497")
            )
        } returns httpResponse

        val resultPatientsByKey = EpicPatientService(epicClient, 100, aidboxClient).findPatientsById(
            tenant,
            mapOf(
                "patient#1" to Identifier(
                    value = "202497",
                    system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                    type = CodeableConcept(text = "MRN")

                ),
                "patient#2" to Identifier(
                    system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                    value = "202497",
                    type = CodeableConcept(text = "MRN")

                )
            )
        )
        assertEquals(validPatientBundle.entry.first().resource as Patient, resultPatientsByKey["patient#1"])
        assertEquals(validPatientBundle.entry.first().resource as Patient, resultPatientsByKey["patient#2"])
    }

    @Test
    fun `ensure, find patient by identifier, request missing system is not returned`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"
        )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|202497")
            )
        } returns httpResponse

        val resultPatientsByKey = EpicPatientService(epicClient, 100, aidboxClient).findPatientsById(
            tenant,
            mapOf(
                "goodIdentifier" to Identifier(
                    value = "202497",
                    system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                    type = CodeableConcept(text = "MRN")
                ),
                "badIdentifier" to Identifier(value = "202497", system = null)
            )
        )
        assertEquals(
            mapOf("goodIdentifier" to validPatientBundle.entry.first().resource), resultPatientsByKey
        )
    }

    @Test
    fun `ensure, find patient by identifier, multiple identifiers matching returns multiple patients`() {
        val validMultiplePatientBundle = readResource<Bundle>("/ExampleMultiPatientBundle.json")
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"
        )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validMultiplePatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084|Z4572,urn:oid:1.2.840.114350.1.13.0.1.7.2.698084|Z5660")
            )
        } returns httpResponse

        val resultPatientsByKey = EpicPatientService(epicClient, 100, aidboxClient).findPatientsById(
            tenant,
            mapOf(
                "patient#1" to Identifier(
                    system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"),
                    value = "Z4572",
                    type = CodeableConcept(text = "EXTERNAL")
                ),
                "patient#2" to Identifier(
                    system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"),
                    value = "Z5660",
                    type = CodeableConcept(text = "EXTERNAL")
                )
            )
        )
        assertEquals(validMultiplePatientBundle.entry[0].resource, resultPatientsByKey["patient#1"])
        assertEquals(validMultiplePatientBundle.entry[1].resource, resultPatientsByKey["patient#2"])
    }

    @Test
    fun `ensure, find patient by identifier, batching works`() {
        val validMultiplePatientBundle = readResource<Bundle>("/ExampleMultiPatientBundle.json")
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
            internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"
        )
        // 1st batch
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validMultiplePatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084|Z4572,urn:oid:1.2.840.114350.1.13.0.1.7.2.698084|Z5660")
            )
        } returns httpResponse

        // 2nd batch
        val httpResponse2 = mockk<HttpResponse>()
        every { httpResponse2.status } returns HttpStatusCode.OK
        coEvery { httpResponse2.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|202497")
            )
        } returns httpResponse2

        val resultPatientsByKey = EpicPatientService(epicClient, 2, aidboxClient).findPatientsById(
            tenant,
            mapOf(
                "patient#1" to Identifier(
                    system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"),
                    value = "Z4572",
                    type = CodeableConcept(text = "EXTERNAL")
                ),
                "patient#2" to Identifier(
                    system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"),
                    value = "Z5660",
                    type = CodeableConcept(text = "EXTERNAL")
                ),
                "patient#3" to Identifier(
                    system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                    value = "202497",
                    type = CodeableConcept(text = "MRN")
                )
            )
        )

        assertEquals(validMultiplePatientBundle.entry[0].resource, resultPatientsByKey["patient#1"])
        assertEquals(validMultiplePatientBundle.entry[1].resource, resultPatientsByKey["patient#2"])
        assertEquals(validPatientBundle.entry[0].resource, resultPatientsByKey["patient#3"])
    }

    @Test
    fun `MRN to FHIR ID retrieval works with patient in aidbox`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
            internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"
        )
        every {
            aidboxClient.getPatientFHIRIds(
                tenant.mnemonic,
                mapOf("key" to SystemValue("MRN", "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"))
            ).getValue("key")
        } returns "TEST_TENANT-FHIRID"
        val response = EpicPatientService(epicClient, 100, aidboxClient).getPatientFHIRId(
            tenant,
            "MRN",
            "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"
        )
        assertEquals(response.fhirID, "FHIRID")
    }

    @Test
    fun `MRN to FHIR ID retrieval works with patient not in aidbox`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
            internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"
        )
        every {
            aidboxClient.getPatientFHIRIds(
                tenant.mnemonic,
                mapOf("key" to SystemValue("MRN", "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"))
            ).getValue("key")
        } throws Exception()

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|MRN")
            )
        } returns httpResponse

        val response = EpicPatientService(epicClient, 100, aidboxClient).getPatientFHIRId(
            tenant,
            "MRN",
            "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"
        )
        assertEquals(response.fhirID, "eJzlzKe3KPzAV5TtkxmNivQ3")
        assertEquals(response.newPatientObject, validPatientBundle.toListOfType<Patient>().first())
    }

    @Test
    fun `MRN to FHIR ID retrieval, multiple patients found`() {
        val tenant = createTestTenant(
            "d45049c3-3441-40ef-ab4d-b9cd86a17225",
            "https://example.org",
            testPrivateKey,
            "TEST_TENANT",
            mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
            internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"
        )
        every {
            aidboxClient.getPatientFHIRIds(
                tenant.mnemonic,
                mapOf("key" to SystemValue("MRN", "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"))
            ).getValue("key")
        } throws Exception()

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validMultiPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|MRN")
            )
        } returns httpResponse

        assertThrows<IOException> {
            EpicPatientService(epicClient, 100, aidboxClient).getPatientFHIRId(
                tenant,
                "MRN",
                "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"
            )
        }
    }
}
