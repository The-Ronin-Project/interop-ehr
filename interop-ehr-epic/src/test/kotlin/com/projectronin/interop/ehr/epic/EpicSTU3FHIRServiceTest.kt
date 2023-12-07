package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import com.projectronin.interop.fhir.stu3.resource.STU3MedicationStatement
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EpicSTU3FHIRServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private val medicationStatementBundle = readResource<STU3Bundle>("/STU3MedicationStatementBundle.json")

    @BeforeEach
    fun setup() {
        epicClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
    }

    @Test
    fun `ensure standard parameters are added when missing`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns medicationStatementBundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val service = TestService(epicClient, mapOf())
        val medicationStatements = service.getMedicationStatements(tenant)
        assertEquals(3, medicationStatements.size)
    }

    @Test
    fun `getById works`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        val medicationStatement = mockk<MedicationStatement>()

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3MedicationStatement>(any()) } returns
            mockk(relaxed = true) {
                every { transformToR4() } returns medicationStatement
            }
        coEvery {
            epicClient.get(
                tenant,
                "url/statement1",
            )
        } returns ehrResponse

        val service = TestService(epicClient, mapOf())
        val response =
            service.getByID(
                tenant,
                "statement1",
            )
        assertEquals(medicationStatement, response)
    }

    @Test
    fun `getByIds works`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns medicationStatementBundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_id" to
                        listOf(
                            "e-Vg66OIhWXBKqYrsP.XNZVYk.Q7JvA7ZsmahScUJ.843",
                            "e10aG32TY3GxQKga7ejscpB-l24kUZEAL4.pKXNJLOlA3",
                            "e8t3zjLeHVt.mv5I5Eu-6.6Wc4BCkPi1DVNUkU8YkOAY3",
                        ),
                ),
            )
        } returns ehrResponse

        val service = TestService(epicClient, mapOf())
        val example =
            service.getByIDs(
                tenant,
                listOf(
                    "e-Vg66OIhWXBKqYrsP.XNZVYk.Q7JvA7ZsmahScUJ.843",
                    "e10aG32TY3GxQKga7ejscpB-l24kUZEAL4.pKXNJLOlA3",
                    "e8t3zjLeHVt.mv5I5Eu-6.6Wc4BCkPi1DVNUkU8YkOAY3",
                ),
            )
        assertEquals(3, example.size)
        assertEquals(
            "e-Vg66OIhWXBKqYrsP.XNZVYk.Q7JvA7ZsmahScUJ.843",
            example["e-Vg66OIhWXBKqYrsP.XNZVYk.Q7JvA7ZsmahScUJ.843"]?.id?.value,
        )
        assertEquals(
            "e10aG32TY3GxQKga7ejscpB-l24kUZEAL4.pKXNJLOlA3",
            example["e10aG32TY3GxQKga7ejscpB-l24kUZEAL4.pKXNJLOlA3"]?.id?.value,
        )
        assertEquals(
            "e8t3zjLeHVt.mv5I5Eu-6.6Wc4BCkPi1DVNUkU8YkOAY3",
            example["e8t3zjLeHVt.mv5I5Eu-6.6Wc4BCkPi1DVNUkU8YkOAY3"]?.id?.value,
        )
    }

    @Test
    fun `ensure standard parameters are not included when already provided`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns medicationStatementBundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 250,
                ),
            )
        } returns ehrResponse

        val service = TestService(epicClient, mapOf("_count" to 250))
        val medicationStatements = service.getMedicationStatements(tenant)
        assertEquals(3, medicationStatements.size)
    }

    @Test
    fun `ensure bundle handles no links`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        val medicationStatement = mockk<MedicationStatement>(relaxed = true)
        val bundle =
            mockk<STU3Bundle>(relaxed = true) {
                every { link } returns listOf()
                every { transformToR4() } returns
                    mockk(relaxed = true) {
                        every { link } returns listOf()
                        every { entry } returns
                            listOf(
                                mockk {
                                    every { resource } returns medicationStatement
                                },
                            )
                    }
            }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns bundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val service = TestService(epicClient, mapOf())
        val medicationStatements = service.getMedicationStatements(tenant)
        assertEquals(1, medicationStatements.size)
    }

    @Test
    fun `ensure bundle handles no next links`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        val medicationStatement = mockk<MedicationStatement>(relaxed = true)
        val bundle =
            mockk<STU3Bundle>(relaxed = true) {
                every { link } returns
                    listOf(
                        mockk {
                            every { relation } returns FHIRString("self")
                        },
                    )
                every { transformToR4() } returns
                    mockk(relaxed = true) {
                        every { link } returns
                            listOf(
                                mockk {
                                    every { relation } returns FHIRString("self")
                                },
                            )
                        every { entry } returns
                            listOf(
                                mockk {
                                    every { resource } returns medicationStatement
                                },
                            )
                    }
            }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns bundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val service = TestService(epicClient, mapOf())
        val medicationStatements = service.getMedicationStatements(tenant)
        assertEquals(1, medicationStatements.size)
    }

    @Test
    fun `ensure bundle handles links with no relations`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        val medicationStatement = mockk<MedicationStatement>(relaxed = true)
        val bundle =
            mockk<STU3Bundle>(relaxed = true) {
                every { link } returns
                    listOf(
                        mockk {
                            every { relation } returns null
                        },
                    )
                every { transformToR4() } returns
                    mockk(relaxed = true) {
                        every { link } returns
                            listOf(
                                mockk {
                                    every { relation } returns null
                                },
                            )
                        every { entry } returns
                            listOf(
                                mockk {
                                    every { resource } returns medicationStatement
                                },
                            )
                    }
            }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns bundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val service = TestService(epicClient, mapOf())
        val medicationStatements = service.getMedicationStatements(tenant)
        assertEquals(1, medicationStatements.size)
    }

    @Test
    fun `ensure bundle handles next link with no URL`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        val medicationStatement = mockk<MedicationStatement>(relaxed = true)
        val bundle =
            mockk<STU3Bundle>(relaxed = true) {
                every { link } returns
                    listOf(
                        mockk {
                            every { relation } returns FHIRString("next")
                            every { url } returns null
                        },
                    )
                every { transformToR4() } returns
                    mockk(relaxed = true) {
                        every { link } returns
                            listOf(
                                mockk {
                                    every { relation } returns FHIRString("next")
                                    every { url } returns null
                                },
                            )
                        every { entry } returns
                            listOf(
                                mockk {
                                    every { resource } returns medicationStatement
                                },
                            )
                    }
            }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns bundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val service = TestService(epicClient, mapOf())
        val medicationStatements = service.getMedicationStatements(tenant)
        assertEquals(1, medicationStatements.size)
    }

    @Test
    fun `ensure bundle handles next link with URL with no value`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        val medicationStatement = mockk<MedicationStatement>(relaxed = true)
        val bundle =
            mockk<STU3Bundle>(relaxed = true) {
                every { link } returns
                    listOf(
                        mockk(relaxed = true) {
                            every { relation } returns FHIRString("next")
                            every { url } returns Uri(null)
                        },
                    )
                every { transformToR4() } returns
                    mockk(relaxed = true) {
                        every { link } returns
                            listOf(
                                mockk(relaxed = true) {
                                    every { relation } returns FHIRString("next")
                                    every { url } returns Uri(null)
                                },
                            )
                        every { entry } returns
                            listOf(
                                mockk(relaxed = true) {
                                    every { resource } returns medicationStatement
                                },
                            )
                    }
            }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns bundle
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val service = TestService(epicClient, mapOf())
        val medicationStatements = service.getMedicationStatements(tenant)
        assertEquals(1, medicationStatements.size)
    }

    @Test
    fun `ensure bundle handles next link with URL`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                "testPrivateKey",
                "TEST_TENANT",
            )

        val medicationStatement1 =
            mockk<MedicationStatement>(relaxed = true) {
                every { id } returns Id("1234")
            }
        val medicationStatement2 =
            mockk<MedicationStatement>(relaxed = true) {
                every { id } returns Id("5678")
            }
        val bundle1 =
            mockk<STU3Bundle>(relaxed = true) {
                every { link } returns
                    listOf(
                        mockk {
                            every { relation } returns FHIRString("next")
                            every { url } returns Uri("http://test/1234")
                        },
                    )
                every { transformToR4() } returns
                    mockk(relaxed = true) {
                        every { link } returns
                            listOf(
                                mockk {
                                    every { relation } returns FHIRString("next")
                                    every { url } returns Uri("http://test/1234")
                                },
                            )
                        every { entry } returns
                            listOf(
                                mockk {
                                    every { resource } returns medicationStatement1
                                },
                            )
                    }
            }
        val bundle2 =
            mockk<STU3Bundle>(relaxed = true) {
                every { link } returns listOf()
                every { transformToR4() } returns
                    mockk(relaxed = true) {
                        every { link } returns listOf()
                        every { entry } returns
                            listOf(
                                mockk {
                                    every { resource } returns medicationStatement2
                                },
                            )
                    }
            }

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns bundle1
        coEvery {
            epicClient.get(
                tenant,
                "url",
                mapOf(
                    "_count" to 50,
                ),
            )
        } returns ehrResponse

        val httpResponse2 =
            mockk<HttpResponse> {
                every { status } returns HttpStatusCode.OK
                coEvery { body<STU3Bundle>() } returns bundle2
            }

        coEvery { epicClient.get(tenant, "http://test/1234") } returns EHRResponse(httpResponse2, "67890")

        val service = TestService(epicClient, mapOf())
        val medicationStatements = service.getMedicationStatements(tenant)
        assertEquals(2, medicationStatements.size)
    }

    private class TestService(
        epicClient: EpicClient,
        private val parameters: Map<String, Any?>,
        override val fhirURLSearchPart: String = "url",
        override val fhirResourceType: Class<MedicationStatement> = MedicationStatement::class.java,
    ) :
        EpicSTU3FHIRService<STU3MedicationStatement, MedicationStatement>(
                epicClient,
                STU3MedicationStatement::class.java,
            ) {
        fun getMedicationStatements(tenant: Tenant): List<MedicationStatement> {
            return getResourceListFromSearchSTU3(tenant, parameters)
        }
    }
}
