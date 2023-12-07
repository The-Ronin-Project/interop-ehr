package com.projectronin.interop.ehr.epic

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

internal class EpicMedicationAdministrationServiceTest {
    private val client: EpicClient = mockk()
    private val ehrda: EHRDataAuthorityClient = mockk()
    private val identifierService: EpicIdentifierService = mockk()
    private val service = EpicMedicationAdministrationService(client, ehrda, identifierService)
    private val testTenant =
        mockk<Tenant> {
            every { mnemonic } returns "testTenant"
            every { vendor } returns
                mockk<Epic> {
                    every { encounterCSNSystem } returns "csnSystem"
                }
        }

    @Test
    fun `getById throws exception`() {
        assertThrows<UnsupportedOperationException> {
            service.getByID(testTenant, "id")
        }
    }

    @Test
    fun `getByIds returns empty map`() {
        val response = service.getByIDs(testTenant, listOf("id", "id2"))
        assertEquals(0, response.size)
    }

    @Test
    fun `happy path`() {
        val medRequest =
            mockk<MedicationRequest> {
                every { subject?.decomposedId() } returns "testTenant-patID"
                every { encounter?.decomposedId() } returns "testTenant-encounterID"
                every { findFhirId() } returns "medReqID"
                every { identifier } returns
                    listOf(
                        mockk {
                            every { system } returns CodeSystem.RONIN_FHIR_ID.uri
                        },
                        mockk {
                            every { system } returns CodeSystem.RONIN_TENANT.uri
                        },
                        mockk {
                            every { system } returns CodeSystem.RONIN_DATA_AUTHORITY.uri
                        },
                        mockk {
                            every { system } returns Uri("orderSystem")
                            every { value?.value } returns "orderID"
                        },
                    )
            }
        coEvery { ehrda.getResourceAs<Patient>(any(), "Patient", "testTenant-patID") } returns
            mockk<Patient> {
                every { identifier } returns listOf(mockk())
            }
        every { identifierService.getPatientIdentifier(testTenant, any()).value?.value } returns "patMRN"
        coEvery {
            ehrda.getResourceAs<Encounter>(
                any(),
                "Encounter",
                "testTenant-encounterID",
            )
        } returns
            mockk<Encounter> {
                every { identifier } returns
                    listOf(
                        mockk {
                            every { system } returns Uri("csnSystem")
                            every { value?.value } returns "encCSN"
                        },
                    )
            }
        every { identifierService.getEncounterIdentifier(testTenant, any()).value?.value } returns "encCSN"
        every { identifierService.getOrderIdentifier(testTenant, any()).value?.value } returns "orderID"

        val epicResponse =
            EpicMedAdmin(
                orders =
                    listOf(
                        EpicMedicationOrder(
                            name = "TEST NAME",
                            medicationAdministrations =
                                listOf(
                                    EpicMedicationAdministration(
                                        administrationInstant = "2011-07-05T14:00:00Z",
                                        action = "Given",
                                        dose = EpicDose(value = "11", unit = "mg"),
                                    ),
                                    EpicMedicationAdministration(
                                        administrationInstant = "",
                                        action = "Given",
                                        dose = EpicDose(value = "15", unit = "mg"),
                                    ),
                                ),
                        ),
                    ),
            )
        coEvery { client.post(testTenant, any(), any()) } returns
            mockk {
                every { sourceURL } returns "https://example.org/medadmin"
                coEvery { body<EpicMedAdmin>() } returns epicResponse
            }
        val result = service.findMedicationAdministrationsByRequest(testTenant, medRequest)
        assertEquals(1, result.size)
        assertEquals("orderID-1309874400", result.first().id?.value)
        assertEquals("https://example.org/medadmin", result.first().meta?.source?.value)
        assertEquals(DynamicValueType.CODEABLE_CONCEPT, result.first().medication?.type)
        assertEquals(CodeableConcept(text = FHIRString("TEST NAME")), result.first().medication?.value)
        assertEquals(Reference(reference = FHIRString("MedicationRequest/medReqID")), result.first().request)
        assertEquals(Reference(reference = FHIRString("Patient/patID")), result.first().subject)
        assertEquals("Given", result.first().status?.value)
        assertEquals(Decimal(BigDecimal("11")), result.first().dosage?.dose?.value)
        assertEquals(FHIRString("mg"), result.first().dosage?.dose?.unit)
    }

    @Test
    fun `patient API returns empty`() {
        assertEquals(
            emptyList<MedicationAdministration>(),
            service.findMedicationAdministrationsByPatient(testTenant, "12345", LocalDate.now(), LocalDate.now()),
        )
    }

    @Test
    fun `test EpicMedAdmin default values`() {
        val epicMedAdmin = EpicMedAdmin()
        assertEquals(emptyList<EpicMedicationOrder>(), epicMedAdmin.orders)
    }

    @Test
    fun `test EpicMedicationOrder default values`() {
        val epicMedicationOrder = EpicMedicationOrder()
        assertEquals(emptyList<EpicMedicationAdministration>(), epicMedicationOrder.medicationAdministrations)
    }

    @Test
    fun `test request code cov`() {
        val request =
            EpicMedAdminRequest(
                patientID = "mrn",
                patientIDType = "Internal",
                contactID = "csn",
                contactIDType = "CSN",
                orderIDs = listOf(EpicOrderID("orderID", "External")),
            )
        assertEquals("mrn", request.patientID)
        assertEquals("Internal", request.patientIDType)
        assertEquals("csn", request.contactID)
        assertEquals("CSN", request.contactIDType)
        assertEquals(1, request.orderIDs.size)
    }

    @Test
    fun `empty list 1`() {
        val medRequest =
            mockk<MedicationRequest> {
                every { subject } returns null
            }
        val result = service.findMedicationAdministrationsByRequest(testTenant, medRequest)
        assertEquals(emptyList<MedicationAdministration>(), result)
    }

    @Test
    fun `empty list 2`() {
        val medRequest =
            mockk<MedicationRequest> {
                every { subject?.decomposedId() } returns "testTenant-patID"
                every { encounter?.decomposedId() } returns "testTenant-encounterID"
                every { findFhirId() } returns "medReqID"
                every { identifier } returns
                    listOf(
                        mockk {
                            every { system } returns CodeSystem.RONIN_FHIR_ID.uri
                        },
                        mockk {
                            every { system } returns CodeSystem.RONIN_TENANT.uri
                        },
                        mockk {
                            every { system } returns CodeSystem.RONIN_DATA_AUTHORITY.uri
                        },
                    )
            }
        coEvery { ehrda.getResourceAs<Patient>(any(), "Patient", "testTenant-patID") } returns
            mockk<Patient> {
                every { identifier } returns listOf(mockk())
            }
        every { identifierService.getPatientIdentifier(testTenant, any()).value?.value } returns "patMRN"
        coEvery {
            ehrda.getResourceAs<Encounter>(
                any(),
                "Encounter",
                "testTenant-encounterID",
            )
        } returns
            mockk<Encounter> {
                every { identifier } returns
                    listOf(
                        mockk {
                            every { system } returns Uri("csnSystem")
                            every { value?.value } returns "encCSN"
                        },
                    )
            }
        every { identifierService.getEncounterIdentifier(testTenant, any()) } returns Identifier(value = null)
        every { identifierService.getOrderIdentifier(testTenant, any()).value?.value } returns "orderID"
        val result = service.findMedicationAdministrationsByRequest(testTenant, medRequest)
        assertEquals(emptyList<MedicationAdministration>(), result)
    }

    @Test
    fun `empty list 3`() {
        val medRequest =
            mockk<MedicationRequest> {
                every { subject?.decomposedId() } returns "testTenant-patID"
                every { encounter?.decomposedId() } returns "testTenant-encounterID"
                every { findFhirId() } returns "medReqID"
                every { identifier } returns listOf(Identifier(value = "something".asFHIR()))
            }
        coEvery { ehrda.getResourceAs<Patient>(any(), "Patient", "testTenant-patID") } returns
            mockk<Patient> {
                every { identifier } returns listOf(mockk())
            }
        every { identifierService.getPatientIdentifier(testTenant, any()).value?.value } returns "patMRN"
        coEvery {
            ehrda.getResourceAs<Encounter>(
                any(),
                "Encounter",
                "testTenant-encounterID",
            )
        } returns
            mockk<Encounter> {
                every { identifier } returns
                    listOf(
                        mockk {
                            every { system } returns Uri("notCsnSystem")
                            every { value?.value } returns "encCSN"
                        },
                    )
            }
        every { identifierService.getEncounterIdentifier(testTenant, any()).value?.value } returns "encCSN"
        every { identifierService.getOrderIdentifier(testTenant, any()) } returns Identifier(value = null)
        val result = service.findMedicationAdministrationsByRequest(testTenant, medRequest)
        assertEquals(emptyList<MedicationAdministration>(), result)
    }

    @Test
    fun `empty list 4`() {
        val medRequest =
            mockk<MedicationRequest> {
                every { subject?.decomposedId() } returns "testTenant-patID"
                every { encounter?.decomposedId() } returns "testTenant-encounterID"
                every { findFhirId() } returns "medReqID"
            }
        coEvery { ehrda.getResourceAs<Patient>(any(), "Patient", "testTenant-patID") } returns
            mockk<Patient> {
                every { identifier } returns listOf(mockk())
            }
        every { identifierService.getPatientIdentifier(testTenant, any()).value?.value } returns "patMRN"
        coEvery { ehrda.getResourceAs<Encounter>(any(), "Encounter", "testTenant-encounterID") } returns null

        val result = service.findMedicationAdministrationsByRequest(testTenant, medRequest)
        assertEquals(emptyList<MedicationAdministration>(), result)
    }

    @Test
    fun `empty list 5`() {
        val medRequest =
            mockk<MedicationRequest> {
                every { subject?.decomposedId() } returns "testTenant-patID"
                every { encounter?.decomposedId() } returns "testTenant-encounterID"
                every { findFhirId() } returns "medReqID"
            }
        coEvery { ehrda.getResourceAs<Patient>(any(), "Patient", "testTenant-patID") } returns
            mockk<Patient> {
                every { identifier } returns listOf(mockk())
            }
        every { identifierService.getPatientIdentifier(testTenant, any()).value } returns null

        val result = service.findMedicationAdministrationsByRequest(testTenant, medRequest)
        assertEquals(emptyList<MedicationAdministration>(), result)
    }

    @Test
    fun `empty list 6`() {
        val medRequest =
            mockk<MedicationRequest> {
                every { subject?.decomposedId() } returns "testTenant-patID"
                every { encounter?.decomposedId() } returns "testTenant-encounterID"
                every { findFhirId() } returns "medReqID"
            }
        coEvery { ehrda.getResourceAs<Patient>(any(), "Patient", "testTenant-patID") } returns null

        val result = service.findMedicationAdministrationsByRequest(testTenant, medRequest)
        assertEquals(emptyList<MedicationAdministration>(), result)
    }

    @Test
    fun `empty list 7`() {
        val medRequest =
            mockk<MedicationRequest> {
                every { subject?.decomposedId() } returns "testTenant-patID"
                every { encounter?.decomposedId() } returns "testTenant-encounterID"
                every { findFhirId() } returns null
            }

        val result = service.findMedicationAdministrationsByRequest(testTenant, medRequest)
        assertEquals(emptyList<MedicationAdministration>(), result)
    }

    @Test
    fun `empty list 8`() {
        val medRequest =
            mockk<MedicationRequest> {
                every { subject?.decomposedId() } returns "testTenant-patID"
                every { encounter?.decomposedId() } returns "testTenant-encounterID"
                every { findFhirId() } returns null
            }

        val result = service.findMedicationAdministrationsByRequest(testTenant, medRequest)
        assertEquals(emptyList<MedicationAdministration>(), result)
    }

    @Test
    fun `empty list 9`() {
        val medRequest =
            mockk<MedicationRequest> {
                every { subject?.decomposedId() } returns "testTenant-patID"
                every { encounter } returns null
            }

        val result = service.findMedicationAdministrationsByRequest(testTenant, medRequest)
        assertEquals(emptyList<MedicationAdministration>(), result)
    }

    @Test
    fun `verify request serialization and deserialization`() {
        val request =
            EpicMedAdminRequest(
                patientID = "PatientId",
                patientIDType = "Internal",
                contactID = "ContactId",
                contactIDType = "CSN",
                orderIDs = listOf(EpicOrderID("OrderId", "External")),
            )

        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request)

        val expectedJson =
            """
            {
              "PatientID" : "PatientId",
              "PatientIDType" : "Internal",
              "ContactID" : "ContactId",
              "ContactIDType" : "CSN",
              "OrderIDs" : [ {
                "ID" : "OrderId",
                "Type" : "External"
              } ]
            }
            """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedRequest = JacksonManager.objectMapper.readValue<EpicMedAdminRequest>(json)
        assertEquals(request, deserializedRequest)
    }
}
