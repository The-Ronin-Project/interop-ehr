package com.projectronin.interop.ehr.epic

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.ehr.dataauthority.models.IdentifierSearchResponse
import com.projectronin.ehr.dataauthority.models.IdentifierSearchableResourceTypes
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.epic.client.EpicClient
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

class EpicPatientServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var ehrdaClient: EHRDataAuthorityClient
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private val validPatientBundle = readResource<Bundle>("/ExamplePatientBundle.json")
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()

    @BeforeEach
    fun initTest() {
        epicClient = mockk()
        ehrdaClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
    }

    @Test
    fun `ensure, find patient, patient is returned`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("given" to "givenName", "family" to "familyName", "birthdate" to "2015-01-01", "_count" to 50),
            )
        } returns ehrResponse

        val bundle =
            EpicPatientService(epicClient, 100, ehrdaClient).findPatient(
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
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
            )
        val fakePat = mockk<Patient>(relaxed = true)
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Patient>(TypeInfo(Patient::class, Patient::class.java)) } returns fakePat
        coEvery { epicClient.get(tenant, "/api/FHIR/R4/Patient/FHIRID") } returns ehrResponse
        val actual = EpicPatientService(epicClient, 100, ehrdaClient).getPatient(tenant, "FHIRID")
        assertEquals(fakePat, actual)
    }

    @Test
    fun `ensure, find patient by identifier, returns single patient`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
            )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf(
                    "identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|202497",
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val resultPatientsByKey =
            EpicPatientService(epicClient, 100, ehrdaClient).findPatientsById(
                tenant,
                mapOf(
                    "patient#1" to
                        Identifier(
                            value = "202497".asFHIR(),
                            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                            type = CodeableConcept(text = "MRN".asFHIR()),
                        ),
                ),
            )
        assertEquals(mapOf("patient#1" to validPatientBundle.entry.first().resource), resultPatientsByKey)
    }

    @Test
    fun `find patient by identifier returns patient with an identifier missing a system and value`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
            )

        val patient =
            mockk<Patient>(relaxed = true) {
                every { identifier } returns
                    listOf(
                        mockk {
                            every { system } returns Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14")
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
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf(
                    "identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|202497",
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val resultPatientsByKey =
            EpicPatientService(epicClient, 100, ehrdaClient).findPatientsById(
                tenant,
                mapOf(
                    "patient#1" to
                        Identifier(
                            value = "202497".asFHIR(),
                            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                            type = CodeableConcept(text = "MRN".asFHIR()),
                        ),
                ),
            )
        assertEquals(1, resultPatientsByKey.size)
        assertNotNull(resultPatientsByKey["patient#1"])
    }

    @Test
    fun `ensure, find patient by identifier, single patient matches multiple requests, each returned the patient`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
            )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf(
                    "identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|202497",
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val resultPatientsByKey =
            EpicPatientService(epicClient, 100, ehrdaClient).findPatientsById(
                tenant,
                mapOf(
                    "patient#1" to
                        Identifier(
                            value = "202497".asFHIR(),
                            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                            type = CodeableConcept(text = "MRN".asFHIR()),
                        ),
                    "patient#2" to
                        Identifier(
                            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                            value = "202497".asFHIR(),
                            type = CodeableConcept(text = "MRN".asFHIR()),
                        ),
                ),
            )
        assertEquals(validPatientBundle.entry.first().resource as Patient, resultPatientsByKey["patient#1"])
        assertEquals(validPatientBundle.entry.first().resource as Patient, resultPatientsByKey["patient#2"])
    }

    @Test
    fun `ensure, find patient by identifier, request missing system is not returned`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
            )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf(
                    "identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|202497",
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val resultPatientsByKey =
            EpicPatientService(epicClient, 100, ehrdaClient).findPatientsById(
                tenant,
                mapOf(
                    "goodIdentifier" to
                        Identifier(
                            value = "202497".asFHIR(),
                            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                            type = CodeableConcept(text = "MRN".asFHIR()),
                        ),
                    "badIdentifier" to Identifier(value = "202497".asFHIR(), system = null),
                ),
            )
        assertEquals(
            mapOf("goodIdentifier" to validPatientBundle.entry.first().resource),
            resultPatientsByKey,
        )
    }

    @Test
    fun `ensure, find patient by identifier, multiple identifiers matching returns multiple patients`() {
        val validMultiplePatientBundle = readResource<Bundle>("/ExampleMultiPatientBundle.json")
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084",
            )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validMultiplePatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf(
                    "identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084|Z4572,urn:oid:1.2.840.114350.1.13.0.1.7.2.698084|Z5660",
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val resultPatientsByKey =
            EpicPatientService(epicClient, 100, ehrdaClient).findPatientsById(
                tenant,
                mapOf(
                    "patient#1" to
                        Identifier(
                            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"),
                            value = "Z4572".asFHIR(),
                            type = CodeableConcept(text = "EXTERNAL".asFHIR()),
                        ),
                    "patient#2" to
                        Identifier(
                            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"),
                            value = "Z5660".asFHIR(),
                            type = CodeableConcept(text = "EXTERNAL".asFHIR()),
                        ),
                ),
            )
        assertEquals(validMultiplePatientBundle.entry[0].resource, resultPatientsByKey["patient#1"])
        assertEquals(validMultiplePatientBundle.entry[1].resource, resultPatientsByKey["patient#2"])
    }

    @Test
    fun `ensure, find patient by identifier, batching works`() {
        val validMultiplePatientBundle = readResource<Bundle>("/ExampleMultiPatientBundle.json")
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
                internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084",
            )
        // 1st batch
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validMultiplePatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf(
                    "identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084|Z4572,urn:oid:1.2.840.114350.1.13.0.1.7.2.698084|Z5660",
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        // 2nd batch
        val httpResponse2 = mockk<HttpResponse>()
        every { httpResponse2.status } returns HttpStatusCode.OK
        coEvery { httpResponse2.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf(
                    "identifier" to "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14|202497",
                    "_count" to 50,
                ),
            )
        } returns EHRResponse(httpResponse2, "67890")

        val resultPatientsByKey =
            EpicPatientService(epicClient, 2, ehrdaClient).findPatientsById(
                tenant,
                mapOf(
                    "patient#1" to
                        Identifier(
                            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"),
                            value = "Z4572".asFHIR(),
                            type = CodeableConcept(text = "EXTERNAL".asFHIR()),
                        ),
                    "patient#2" to
                        Identifier(
                            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.2.698084"),
                            value = "Z5660".asFHIR(),
                            type = CodeableConcept(text = "EXTERNAL".asFHIR()),
                        ),
                    "patient#3" to
                        Identifier(
                            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"),
                            value = "202497".asFHIR(),
                            type = CodeableConcept(text = "MRN".asFHIR()),
                        ),
                ),
            )

        assertEquals(validMultiplePatientBundle.entry[0].resource, resultPatientsByKey["patient#1"])
        assertEquals(validMultiplePatientBundle.entry[1].resource, resultPatientsByKey["patient#2"])
        assertEquals(validPatientBundle.entry[0].resource, resultPatientsByKey["patient#3"])
    }

    @Test
    fun `getPatientsFHIRIds works with patient in ehrda`() {
        val mrn = "MRN"
        val fhirID = "FHIRID"
        val mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"

        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = mrnSystem,
                internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084",
            )
        val mockResponse =
            mockk<IdentifierSearchResponse> {
                every { foundResources } returns
                    listOf(
                        mockk {
                            every { searchedIdentifier.value } returns mrn
                            every { identifiers } returns
                                listOf(
                                    mockk {
                                        every { system } returns CodeSystem.RONIN_FHIR_ID.uri.value!!
                                        every { value } returns fhirID
                                    },
                                )
                        },
                    )
            }
        coEvery {
            ehrdaClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(EHRDAIdentifier(value = mrn, system = mrnSystem)),
            )
        } returns listOf(mockResponse)

        val response =
            EpicPatientService(epicClient, 100, ehrdaClient).getPatientsFHIRIds(
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
        val mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"

        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = mrnSystem,
                internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084",
            )

        val mockResponse =
            mockk<IdentifierSearchResponse> {
                every { foundResources } returns
                    listOf(
                        mockk {
                            every { searchedIdentifier.value } returns mrn
                            every { identifiers } returns
                                listOf(
                                    mockk {
                                        every { system } returns CodeSystem.RONIN_FHIR_ID.uri.value!!
                                        every { value } returns fhirID
                                    },
                                )
                        },
                    )
            }
        coEvery {
            ehrdaClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(EHRDAIdentifier(value = mrn, system = mrnSystem)),
            )
        } returns listOf(mockResponse)

        val response =
            EpicPatientService(epicClient, 100, ehrdaClient).getPatientFHIRId(
                tenant,
                mrn,
            )

        assertEquals(fhirID, response)
    }

    @Test
    fun `getPatientsFHIRIds works with patient not in ehrda`() {
        val mrn = "MRN"
        val fhirID = "FHIRID"
        val mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"

        val epicPatientService = spyk(EpicPatientService(epicClient, 100, ehrdaClient))

        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = mrnSystem,
                internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084",
            )

        coEvery {
            ehrdaClient.getResourceIdentifiers(
                any(),
                any(),
                any(),
            )
        } returns listOf()

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
        val mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"
        val aidBoxMRN1 = "AB_MRN1"
        val aidBoxMRN2 = "AB_MRN2"
        val aidBoxFhirId1 = "AB_FHIR_1"
        val aidBoxFhirId2 = "AB_FHIR_2"
        val ehrMRN1 = "EHR_MRN1"
        val ehrMRN2 = "EHR_MRN2"
        val ehrFhirId1 = "EHRFHIR1"
        val ehrFhirId2 = "EHRFHIR2"

        val epicPatientService = spyk(EpicPatientService(epicClient, 100, ehrdaClient))

        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = mrnSystem,
                internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084",
            )

        val mockResponse1 =
            mockk<IdentifierSearchResponse> {
                every { foundResources } returns
                    listOf(
                        mockk {
                            every { identifiers } returns
                                listOf(
                                    mockk {
                                        every { system } returns CodeSystem.RONIN_FHIR_ID.uri.value!!
                                        every { value } returns aidBoxFhirId1
                                    },
                                )
                        },
                    )
                every { searchedIdentifier.value } returns aidBoxMRN1
            }
        val mockResponse2 =
            mockk<IdentifierSearchResponse> {
                every { foundResources } returns
                    listOf(
                        mockk {
                            every { identifiers } returns
                                listOf(
                                    mockk {
                                        every { system } returns CodeSystem.RONIN_FHIR_ID.uri.value!!
                                        every { value } returns aidBoxFhirId2
                                    },
                                )
                        },
                    )
                every { searchedIdentifier.value } returns aidBoxMRN2
            }
        val mockResponse3 =
            mockk<IdentifierSearchResponse> {
                every { foundResources } returns listOf()
                every { searchedIdentifier.value } returns ehrMRN1
            }
        val mockResponse4 =
            mockk<IdentifierSearchResponse> {
                every { foundResources } returns listOf()
                every { searchedIdentifier.value } returns ehrMRN2
            }

        coEvery {
            ehrdaClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(
                    EHRDAIdentifier(value = aidBoxMRN1, system = mrnSystem),
                    EHRDAIdentifier(value = aidBoxMRN2, system = mrnSystem),
                    EHRDAIdentifier(value = ehrMRN1, system = mrnSystem),
                    EHRDAIdentifier(value = ehrMRN2, system = mrnSystem),
                ),
            )
        } returns listOf(mockResponse1, mockResponse2, mockResponse3, mockResponse4)

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
        val mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"

        val epicPatientService = spyk(EpicPatientService(epicClient, 100, ehrdaClient))

        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = mrnSystem,
                internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084",
            )
        val mockResponse =
            mockk<IdentifierSearchResponse> {
                every { foundResources } returns listOf()
                every { searchedIdentifier.value } returns mrn
            }

        coEvery {
            ehrdaClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(
                    EHRDAIdentifier(value = mrn, system = mrnSystem),
                ),
            )
        } returns listOf(mockResponse)

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
    fun `getPatientsFHIRId handles no patients found`() {
        val mrn = "MRN"
        val mrnSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14"

        val epicPatientService = spyk(EpicPatientService(epicClient, 100, ehrdaClient))

        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                mrnSystem = mrnSystem,
                internalSystem = "urn:oid:1.2.840.114350.1.13.0.1.7.2.698084",
            )

        val mockResponse =
            mockk<IdentifierSearchResponse> {
                every { foundResources } returns listOf()
                every { searchedIdentifier.value } returns mrn
            }

        coEvery {
            ehrdaClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                listOf(
                    EHRDAIdentifier(value = mrn, system = mrnSystem),
                ),
            )
        } returns listOf(mockResponse)

        every {
            epicPatientService.findPatientsById(
                tenant,
                mapOf(mrn to Identifier(value = mrn.asFHIR(), system = Uri(mrnSystem))),
            )
        } returns mapOf()

        val exception =
            assertThrows<VendorIdentifierNotFoundException> { epicPatientService.getPatientFHIRId(tenant, mrn) }

        assertEquals("No FHIR ID found for patient", exception.message)
    }

    @Test
    fun `find patient works with disable retry set to true`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("given" to "givenName", "family" to "familyName", "birthdate" to "2015-01-01", "_count" to 50),
                true,
            )
        } returns ehrResponse

        val bundle =
            EpicPatientService(epicClient, 100, ehrdaClient).findPatient(
                tenant,
                LocalDate.of(2015, 1, 1),
                "givenName",
                "familyName",
                true,
            )
        assertEquals(validPatientBundle.entry.map { it.resource }.filterIsInstance<Patient>(), bundle)
    }

    @Test
    fun `find patient works with disable retry set to false`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("given" to "givenName", "family" to "familyName", "birthdate" to "2015-01-01", "_count" to 50),
                false,
            )
        } returns ehrResponse

        val bundle =
            EpicPatientService(epicClient, 100, ehrdaClient).findPatient(
                tenant,
                LocalDate.of(2015, 1, 1),
                "givenName",
                "familyName",
                false,
            )
        assertEquals(validPatientBundle.entry.map { it.resource }.filterIsInstance<Patient>(), bundle)
    }

    @Test
    fun `find patient works with disable retry not provided`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validPatientBundle
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/Patient",
                mapOf("given" to "givenName", "family" to "familyName", "birthdate" to "2015-01-01", "_count" to 50),
                false,
            )
        } returns ehrResponse

        val bundle =
            EpicPatientService(epicClient, 100, ehrdaClient).findPatient(
                tenant,
                LocalDate.of(2015, 1, 1),
                "givenName",
                "familyName",
            )
        assertEquals(validPatientBundle.entry.map { it.resource }.filterIsInstance<Patient>(), bundle)
    }
}
