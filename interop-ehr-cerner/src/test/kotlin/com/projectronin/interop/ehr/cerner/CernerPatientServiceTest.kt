package com.projectronin.interop.ehr.cerner

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.ehr.dataauthority.models.IdentifierSearchResponse
import com.projectronin.ehr.dataauthority.models.IdentifierSearchableResourceTypes
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Patient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.util.reflect.TypeInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import com.projectronin.ehr.dataauthority.models.Identifier as EHRDAIdentifier

class CernerPatientServiceTest {
    private lateinit var cernerClient: CernerClient
    private lateinit var ehrDataAuthorityClient: EHRDataAuthorityClient
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse

    private val validPatientBundle = readResource<Bundle>("/ExamplePatientBundle.json")
    private val validPatientBundleSingle = readResource<Bundle>("/ExamplePatientBundleSingle.json")

    @BeforeEach
    fun initTest() {
        cernerClient = mockk()
        ehrDataAuthorityClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
    }

    @Test
    fun `findPatient patient is returned`() {
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Patient",
                mapOf(
                    "given" to "givenName",
                    "family:exact" to "familyName",
                    "birthdate" to "2015-01-01",
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val bundle =
            CernerPatientService(cernerClient, ehrDataAuthorityClient).findPatient(
                tenant,
                LocalDate.of(2015, 1, 1),
                "givenName",
                "familyName",
            )
        assertEquals(validPatientBundle.entry.map { it.resource }.filterIsInstance<Patient>(), bundle)
    }

    @Test
    fun `getPatient - works`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )
        val fakePat = mockk<Patient>(relaxed = true)
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Patient>(TypeInfo(Patient::class, Patient::class.java)) } returns fakePat
        coEvery { cernerClient.get(tenant, "/Patient/FHIRID") } returns ehrResponse
        val actual = CernerPatientService(cernerClient, ehrDataAuthorityClient).getPatient(tenant, "FHIRID")
        assertEquals(fakePat, actual)
    }

    @Test
    fun `findPatientsById returns single patient`() {
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundleSingle
        coEvery {
            cernerClient.get(
                tenant,
                "/Patient",
                mapOf(
                    "identifier" to "urn:oid:2.16.840.1.113883.6.1000|9299",
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val resultPatientsByKey =
            CernerPatientService(cernerClient, ehrDataAuthorityClient).findPatientsById(
                tenant,
                mapOf(
                    "patient#1" to
                        Identifier(
                            value = "9299".asFHIR(),
                            system = Uri("urn:oid:2.16.840.1.113883.6.1000"),
                        ),
                ),
            )
        assertEquals(mapOf("patient#1" to validPatientBundleSingle.entry.first().resource), resultPatientsByKey)
    }

    @Test
    fun `findPatientsById returns patient with an identifier missing a system and value`() {
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )

        val patient =
            mockk<Patient>(relaxed = true) {
                every { identifier } returns
                    listOf(
                        mockk {
                            every { system } returns Uri("urn:oid:2.16.840.1.113883.6.1000")
                            every { value } returns FHIRString("202497")
                        },
                        mockk {
                            every { system } returns Uri("value-less")
                            every { value } returns null
                        },
                    )
            }
        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { link } returns listOf()
                every { entry } returns
                    listOf(
                        mockk {
                            every { resource } returns patient
                        },
                    )
            }
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Patient",
                mapOf(
                    "identifier" to "urn:oid:2.16.840.1.113883.6.1000|202497",
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val resultPatientsByKey =
            CernerPatientService(cernerClient, ehrDataAuthorityClient).findPatientsById(
                tenant,
                mapOf(
                    "patient#1" to
                        Identifier(
                            value = "202497".asFHIR(),
                            system = Uri("urn:oid:2.16.840.1.113883.6.1000"),
                            type = CodeableConcept(text = "MRN".asFHIR()),
                        ),
                ),
            )
        assertEquals(1, resultPatientsByKey.size)
        assertNotNull(resultPatientsByKey["patient#1"])
    }

    @Test
    fun `findPatientsById returns emptyMap when nothing found`() {
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )

        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { link } returns listOf()
                every { entry } returns emptyList()
            }
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns bundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Patient",
                mapOf(
                    "identifier" to "urn:oid:2.16.840.1.113883.6.1000|202497",
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val resultPatientsByKey =
            CernerPatientService(cernerClient, ehrDataAuthorityClient).findPatientsById(
                tenant,
                mapOf(
                    "patient#1" to
                        Identifier(
                            value = "202497".asFHIR(),
                            system = Uri("urn:oid:2.16.840.1.113883.6.1000"),
                            type = CodeableConcept(text = "MRN".asFHIR()),
                        ),
                ),
            )
        assertEquals(0, resultPatientsByKey.size)
    }

    @Test
    fun `findPatientsById request missing system is not returned`() {
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundleSingle
        coEvery {
            cernerClient.get(
                tenant,
                "/Patient",
                mapOf(
                    "identifier" to "urn:oid:2.16.840.1.113883.6.1000|9299",
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val resultPatientsByKey =
            CernerPatientService(cernerClient, ehrDataAuthorityClient).findPatientsById(
                tenant,
                mapOf(
                    "goodIdentifier" to
                        Identifier(
                            value = "9299".asFHIR(),
                            system = Uri("urn:oid:2.16.840.1.113883.6.1000"),
                            type = CodeableConcept(text = "MRN".asFHIR()),
                        ),
                    "badIdentifier" to Identifier(value = "202497".asFHIR(), system = null),
                ),
            )
        assertEquals(
            mapOf("goodIdentifier" to validPatientBundleSingle.entry.first().resource),
            resultPatientsByKey,
        )
    }

    @Test
    fun `findPatientsById throws error for bad input`() {
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )
        assertThrows<java.lang.NullPointerException> {
            CernerPatientService(cernerClient, ehrDataAuthorityClient).findPatientsById(
                tenant,
                mapOf(
                    "badIdentifier" to Identifier(value = null, system = Uri("Test")),
                ),
            )
        }
    }

    @Test
    fun `getPatientsFHIRIds works with patient in ehrda`() {
        val mrn = "MRN"
        val fhirID = "FHIRID"
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )
        val mockResponse =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier } returns EHRDAIdentifier(value = mrn, system = mrnSystem)
                    every { foundResources } returns
                        listOf(
                            mockk {
                                every { identifiers } returns
                                    listOf(
                                        mockk {
                                            every { value } returns fhirID
                                            every { system } returns CodeSystem.RONIN_FHIR_ID.uri.value!!
                                        },
                                    )
                            },
                        )
                },
            )
        coEvery {
            ehrDataAuthorityClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(EHRDAIdentifier(value = mrn, system = mrnSystem)),
            )
        } returns mockResponse

        val response =
            CernerPatientService(cernerClient, ehrDataAuthorityClient).getPatientsFHIRIds(
                tenant,
                mrnSystem,
                listOf(mrn),
            )

        assertEquals(fhirID, response[mrn]!!.fhirID)
    }

    @Test
    fun `getPatientFHIRId works with patient in ehrda`() {
        val mrn = "MRN"
        val fhirID = "FHIRID"
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )

        val mockResponse =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier } returns EHRDAIdentifier(value = mrn, system = mrnSystem)
                    every { foundResources } returns
                        listOf(
                            mockk {
                                every { identifiers } returns
                                    listOf(
                                        mockk {
                                            every { value } returns fhirID
                                            every { system } returns CodeSystem.RONIN_FHIR_ID.uri.value!!
                                        },
                                    )
                            },
                        )
                },
            )
        coEvery {
            ehrDataAuthorityClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(EHRDAIdentifier(value = mrn, system = mrnSystem)),
            )
        } returns mockResponse

        val response =
            CernerPatientService(cernerClient, ehrDataAuthorityClient).getPatientFHIRId(
                tenant,
                mrn,
            )

        assertEquals(fhirID, response)
    }

    @Test
    fun `getPatientFHIRId errors with no patient id`() {
        val mrn = "MRN"
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val cernerPatientService = spyk(CernerPatientService(cernerClient, ehrDataAuthorityClient))

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )

        val mockResponse =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier } returns EHRDAIdentifier(value = mrn, system = mrnSystem)
                    every { foundResources } returns listOf()
                },
            )
        coEvery {
            ehrDataAuthorityClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(EHRDAIdentifier(value = mrn, system = mrnSystem)),
            )
        } returns mockResponse

        every {
            cernerPatientService.findPatientsById(
                tenant,
                mapOf(mrn to Identifier(value = mrn.asFHIR(), system = Uri(mrnSystem))),
            )
        } returns emptyMap()

        assertThrows<VendorIdentifierNotFoundException> {
            cernerPatientService.getPatientFHIRId(
                tenant,
                mrn,
            )
        }
    }

    @Test
    fun `getPatientsFHIRIds works with patient not in ehrda`() {
        val mrn = "MRN"
        val fhirID = "FHIRID"
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val epicPatientService = spyk(CernerPatientService(cernerClient, ehrDataAuthorityClient))

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )
        val mockResponse =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier } returns EHRDAIdentifier(value = mrn, system = mrnSystem)
                    every { foundResources } returns listOf()
                },
            )
        coEvery {
            ehrDataAuthorityClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(EHRDAIdentifier(value = mrn, system = mrnSystem)),
            )
        } returns mockResponse

        every {
            epicPatientService.findPatientsById(
                tenant,
                mapOf(mrn to Identifier(value = mrn.asFHIR(), system = Uri(mrnSystem))),
            )
        } returns mapOf(mrn to Patient(id = Id(fhirID)))

        val response =
            epicPatientService.getPatientsFHIRIds(
                tenant,
                mrnSystem,
                listOf(mrn),
            )

        assertEquals(fhirID, response[mrn]!!.fhirID)
    }

    @Test
    fun `getPatientsFHIRIds works with a mix of patients in and out of ehrda`() {
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"
        val aidBoxMRN1 = "AB_MRN1"
        val aidBoxMRN2 = "AB_MRN2"
        val aidBoxFhirId1 = "AB_FHIR_1"
        val aidBoxFhirId2 = "AB_FHIR_2"
        val ehrMRN1 = "EHR_MRN1"
        val ehrMRN2 = "EHR_MRN2"
        val ehrFhirId1 = "EHRFHIR1"
        val ehrFhirId2 = "EHRFHIR2"

        val epicPatientService = spyk(CernerPatientService(cernerClient, ehrDataAuthorityClient))

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )
        val searchId1 = EHRDAIdentifier(value = aidBoxMRN1, system = mrnSystem)
        val searchId2 = EHRDAIdentifier(value = aidBoxMRN2, system = mrnSystem)
        val searchId3 = EHRDAIdentifier(value = ehrMRN1, system = mrnSystem)
        val searchId4 = EHRDAIdentifier(value = ehrMRN2, system = mrnSystem)
        val mockResponse1 =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier } returns searchId1
                    every { foundResources } returns
                        listOf(
                            mockk {
                                every { identifiers } returns
                                    listOf(
                                        mockk {
                                            every { value } returns aidBoxFhirId1
                                            every { system } returns CodeSystem.RONIN_FHIR_ID.uri.value!!
                                        },
                                    )
                            },
                        )
                },
            )
        val mockResponse2 =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier } returns searchId2
                    every { foundResources } returns
                        listOf(
                            mockk {
                                every { identifiers } returns
                                    listOf(
                                        mockk {
                                            every { value } returns aidBoxFhirId2
                                            every { system } returns CodeSystem.RONIN_FHIR_ID.uri.value!!
                                        },
                                    )
                            },
                        )
                },
            )
        val mockResponse3 =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier } returns searchId3
                    every { foundResources } returns listOf()
                },
            )
        val mockResponse4 =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier } returns searchId4
                    every { foundResources } returns listOf()
                },
            )
        coEvery {
            ehrDataAuthorityClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(searchId1, searchId2, searchId3, searchId4),
            )
        } returns mockResponse1 + mockResponse2 + mockResponse3 + mockResponse4

        every {
            epicPatientService.findPatientsById(
                tenant,
                mapOf(
                    ehrMRN1 to Identifier(value = ehrMRN1.asFHIR(), system = Uri(mrnSystem)),
                    ehrMRN2 to Identifier(value = ehrMRN2.asFHIR(), system = Uri(mrnSystem)),
                ),
            )
        } returns
            mapOf(
                ehrMRN1 to Patient(id = Id(ehrFhirId1)),
                ehrMRN2 to Patient(id = Id(ehrFhirId2)),
            )

        val response =
            epicPatientService.getPatientsFHIRIds(
                tenant,
                mrnSystem,
                listOf(aidBoxMRN1, aidBoxMRN2, ehrMRN1, ehrMRN2),
            )

        assertEquals(4, response.size)
        assertEquals(aidBoxFhirId1, response[aidBoxMRN1]!!.fhirID)
        assertEquals(aidBoxFhirId2, response[aidBoxMRN2]!!.fhirID)
        assertEquals(ehrFhirId1, response[ehrMRN1]!!.fhirID)
        assertEquals(ehrFhirId1, response[ehrMRN1]!!.newPatientObject!!.id!!.value)
        assertEquals(ehrFhirId2, response[ehrMRN2]!!.fhirID)
        assertEquals(ehrFhirId2, response[ehrMRN2]!!.newPatientObject!!.id!!.value)
    }

    @Test
    fun `getPatientsFHIRIds handles no patients found`() {
        val mrn = "MRN"
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val epicPatientService = spyk(CernerPatientService(cernerClient, ehrDataAuthorityClient))

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
            )

        val mockResponse =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier } returns EHRDAIdentifier(value = mrn, system = mrnSystem)
                    every { foundResources } returns listOf()
                },
            )
        coEvery {
            ehrDataAuthorityClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(EHRDAIdentifier(value = mrn, system = mrnSystem)),
            )
        } returns mockResponse

        every {
            epicPatientService.findPatientsById(
                tenant,
                mapOf(mrn to Identifier(value = mrn.asFHIR(), system = Uri(mrnSystem))),
            )
        } returns mapOf()

        val response =
            epicPatientService.getPatientsFHIRIds(
                tenant,
                mrnSystem,
                listOf(mrn),
            )

        assertEquals(0, response.size)
    }

    @Test
    fun `findPatient supports disable retry being true`() {
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Patient",
                mapOf(
                    "given" to "givenName",
                    "family:exact" to "familyName",
                    "birthdate" to "2015-01-01",
                    "_count" to 20,
                ),
                true,
            )
        } returns ehrResponse

        val bundle =
            CernerPatientService(cernerClient, ehrDataAuthorityClient).findPatient(
                tenant,
                LocalDate.of(2015, 1, 1),
                "givenName",
                "familyName",
                true,
            )
        assertEquals(validPatientBundle.entry.map { it.resource }.filterIsInstance<Patient>(), bundle)
    }

    @Test
    fun `findPatient supports disable retry being false`() {
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Patient",
                mapOf(
                    "given" to "givenName",
                    "family:exact" to "familyName",
                    "birthdate" to "2015-01-01",
                    "_count" to 20,
                ),
                false,
            )
        } returns ehrResponse

        val bundle =
            CernerPatientService(cernerClient, ehrDataAuthorityClient).findPatient(
                tenant,
                LocalDate.of(2015, 1, 1),
                "givenName",
                "familyName",
                false,
            )
        assertEquals(validPatientBundle.entry.map { it.resource }.filterIsInstance<Patient>(), bundle)
    }

    @Test
    fun `findPatient supports disable retry being not set`() {
        val mrnSystem = "urn:oid:2.16.840.1.113883.6.1000"

        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                mrnSystem = mrnSystem,
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Patient",
                mapOf(
                    "given" to "givenName",
                    "family:exact" to "familyName",
                    "birthdate" to "2015-01-01",
                    "_count" to 20,
                ),
                false,
            )
        } returns ehrResponse

        val bundle =
            CernerPatientService(cernerClient, ehrDataAuthorityClient).findPatient(
                tenant,
                LocalDate.of(2015, 1, 1),
                "givenName",
                "familyName",
            )
        assertEquals(validPatientBundle.entry.map { it.resource }.filterIsInstance<Patient>(), bundle)
    }
}
