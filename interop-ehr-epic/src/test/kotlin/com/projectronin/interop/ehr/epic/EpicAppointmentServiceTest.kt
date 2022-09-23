package com.projectronin.interop.ehr.epic

import com.projectronin.interop.aidbox.PatientService
import com.projectronin.interop.aidbox.PractitionerService
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.common.http.ktor.throwExceptionFromHttpStatus
import com.projectronin.interop.ehr.epic.apporchard.model.EpicAppointment
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProvider
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.outputs.GetFHIRIDResponse
import com.projectronin.interop.ehr.util.asCode
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.datatype.BundleEntry
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.BundleType
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class EpicAppointmentServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var patientService: EpicPatientService
    private lateinit var identifierService: EpicIdentifierService
    private lateinit var aidboxPractitionerService: PractitionerService
    private lateinit var aidboxPatientService: PatientService

    private lateinit var httpResponse: HttpResponse
    private val validPatientAppointmentSearchResponse =
        readResource<STU3Bundle>("/ExampleFHIRAppointmentBundle.json")
    private val validProviderAppointmentSearchResponse =
        readResource<GetAppointmentsResponse>("/ExampleProviderAppointmentBundle.json")
    private val validOldPatientAppointmentSearchResponse =
        readResource<GetAppointmentsResponse>("/ExampleAppointmentBundle.json")
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()
    private val goodIdentifier = Identifier(
        value = "E1000",
        system = Uri("providerSystem"),
        type = CodeableConcept(
            text = "external"
        )
    )
    private val goodProviderFHIRIdentifier = FHIRIdentifiers(
        id = Id("test"),
        identifiers = listOf(goodIdentifier)
    )
    private val patientFhirAppointmentSearchUrlPart = "/api/FHIR/STU3/Appointment"
    private val patientAppointmentSearchUrlPart =
        "/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments"
    private val providerAppointmentSearchUrlPart =
        "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments"
    private val singleAppointmentBundle = Bundle(
        id = null,
        entry = listOf(
            BundleEntry(
                resource = Appointment(
                    id = Id("123"),
                    status = AppointmentStatus.BOOKED.asCode(),
                    participant = listOf()
                )
            )
        ),
        type = BundleType.TRANSACTION_RESPONSE.asCode()
    )
    private val multipleAppointmentBundle = Bundle(
        id = null,
        entry = listOf(
            BundleEntry(
                resource = Appointment(
                    id = Id("123"),
                    status = AppointmentStatus.BOOKED.asCode(),
                    participant = listOf()
                )
            ),
            BundleEntry(
                resource = Appointment(
                    id = Id("456"),
                    status = AppointmentStatus.BOOKED.asCode(),
                    participant = listOf()
                )
            )
        ),
        type = BundleType.TRANSACTION_RESPONSE.asCode()
    )

    @BeforeEach
    fun initTest() {
        epicClient = mockk()
        httpResponse = mockk()
        patientService = mockk()
        identifierService = mockk()
        aidboxPractitionerService = mockk()
        aidboxPatientService = mockk()
    }

    @Test
    fun `findPatientAppointments - ensure patient appointments are returned`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns validPatientAppointmentSearchResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/STU3/Appointment",
                mapOf(
                    "patient" to "E5597",
                    "status" to "booked",
                    "date" to listOf("ge2015-01-01", "le2015-11-01")
                )
            )
        } returns httpResponse

        val bundle =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                true
            ).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        assertEquals(validPatientAppointmentSearchResponse.transformToR4().toListOfType<Appointment>(), bundle)
    }

    @Test
    fun `findProviderAppointments - ensure provider appointments are returned`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                true
            )
        )

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns validProviderAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments",
                GetProviderAppointmentRequest(
                    userID = "ehrUserId",
                    providers = listOf(ScheduleProvider(id = "E1000")),
                    startDate = "01/01/2015",
                    endDate = "11/01/2015"
                )
            )
        } returns httpResponse

        // Patient service request
        every {
            patientService.getPatientsFHIRIds(
                tenant = tenant,
                patientIDSystem = tenant.vendorAs<Epic>().patientInternalSystem,
                patientIDValues = listOf("     Z6156", "     Z6740", "     Z6783", "     Z4575")
            )
        } returns mapOf(
            "     Z6156" to GetFHIRIDResponse("fhirID1"),
            "     Z6740" to GetFHIRIDResponse("fhirID2"),
            "     Z6783" to GetFHIRIDResponse("fhirID3"),
            "     Z4575" to GetFHIRIDResponse("fhirID4")
        )

        // STU3 appointment search
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientFhirAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID1",
                    "identifier" to "csnSystem|38033,csnSystem|38035"
                )
            )
        } returns multipleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientFhirAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID2",
                    "identifier" to "csnSystem|38034,csnSystem|38036"
                )
            )
        } returns multipleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientFhirAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID3",
                    "identifier" to "csnSystem|38037"
                )
            )
        } returns singleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientFhirAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID4",
                    "identifier" to "csnSystem|38184"
                )
            )
        } returns singleAppointmentBundle

        val response =
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        assertEquals(6, response.appointments.size)
        assertEquals("123", response.appointments[0].id!!.value)
        assertEquals("456", response.appointments[1].id!!.value)
        assertEquals("123", response.appointments[2].id!!.value)
        assertEquals("456", response.appointments[3].id!!.value)
        assertEquals("123", response.appointments[4].id!!.value)
        assertEquals("123", response.appointments[5].id!!.value)
        assertTrue(response.newPatients!!.isEmpty())
    }

    @Test
    fun `findProviderAppointments - ensure provider appointments returns new patients`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                true
            )
        )

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns validProviderAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments",
                GetProviderAppointmentRequest(
                    userID = "ehrUserId",
                    providers = listOf(ScheduleProvider(id = "E1000")),
                    startDate = "01/01/2015",
                    endDate = "11/01/2015"
                )
            )
        } returns httpResponse

        // Patient service request
        every {
            patientService.getPatientsFHIRIds(
                tenant = tenant,
                patientIDSystem = tenant.vendorAs<Epic>().patientInternalSystem,
                patientIDValues = listOf("     Z6156", "     Z6740", "     Z6783", "     Z4575")
            )
        } returns mapOf(
            "     Z6156" to GetFHIRIDResponse("fhirID1", Patient(id = Id("123"))),
            "     Z6740" to GetFHIRIDResponse("fhirID2", Patient(id = Id("456"))),
            "     Z6783" to GetFHIRIDResponse("fhirID3"),
            "     Z4575" to GetFHIRIDResponse("fhirID4")
        )

        // STU3 appointment search
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientFhirAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID1",
                    "identifier" to "csnSystem|38033,csnSystem|38035"
                )
            )
        } returns multipleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientFhirAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID2",
                    "identifier" to "csnSystem|38034,csnSystem|38036"
                )
            )
        } returns multipleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientFhirAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID3",
                    "identifier" to "csnSystem|38037"
                )
            )
        } returns singleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientFhirAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID4",
                    "identifier" to "csnSystem|38184"
                )
            )
        } returns singleAppointmentBundle

        val response =
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        assertEquals(2, response.newPatients!!.size)

        val newPatients = response.newPatients!!
        assertEquals("123", newPatients[0].id!!.value)
        assertEquals("456", newPatients[1].id!!.value)
    }

    @Test
    fun `findProviderAppointments - ensure provider appointments handles failed GetProviderAppointments call `() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                true
            )
        )

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        every {
            httpResponse.throwExceptionFromHttpStatus("GetProviderAppointments", providerAppointmentSearchUrlPart)
        } throws Exception("exception")

        assertThrows<Exception> {
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        }
    }

    @Test
    fun `findProviderAppointments- ensure provider appointments handles failed identifier service call `() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                true
            )
        )

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns Identifier(value = null)

        assertThrows<VendorIdentifierNotFoundException> {
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        }
    }

    @Test
    fun `findProviderAppointments- ensure provider appointments handles patient FHIR id not found`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                true
            )
        )

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns validProviderAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments",
                GetProviderAppointmentRequest(
                    userID = "ehrUserId",
                    providers = listOf(ScheduleProvider(id = "E1000")),
                    startDate = "01/01/2015",
                    endDate = "11/01/2015"
                )
            )
        } returns httpResponse

        // Patient service request
        every {
            patientService.getPatientsFHIRIds(
                tenant = tenant,
                patientIDSystem = tenant.vendorAs<Epic>().patientInternalSystem,
                patientIDValues = listOf("     Z6156", "     Z6740", "     Z6783", "     Z4575")
            )
        } returns mapOf()

        val exception = assertThrows<VendorIdentifierNotFoundException> {
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        }

        assertEquals("FHIR ID not found for patient      Z6156", exception.message)
    }

    @Test
    fun `findProviderAppointments - ensure provider appointments handles no appointments found`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                true
            )
        )

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns GetAppointmentsResponse(
            appointments = listOf(),
            error = null
        )
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments",
                GetProviderAppointmentRequest(
                    userID = "ehrUserId",
                    providers = listOf(ScheduleProvider(id = "E1000")),
                    startDate = "01/01/2015",
                    endDate = "11/01/2015"
                )
            )
        } returns httpResponse

        // Patient service request
        every {
            patientService.getPatientsFHIRIds(
                tenant = tenant,
                patientIDSystem = tenant.vendorAs<Epic>().patientInternalSystem,
                patientIDValues = listOf()
            )
        } returns mapOf()

        val response = epicAppointmentService.findProviderAppointments(
            tenant,
            listOf(goodProviderFHIRIdentifier),
            LocalDate.of(2015, 1, 1),
            LocalDate.of(2015, 11, 1)
        )

        assertEquals(0, response.appointments.size)
    }

    @Test
    fun `findPatientAppointments - ensure patient appointments are returned old API`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        val existingIdentifiers = mockk<List<Identifier>> {}
        every { aidboxPatientService.getPatient(tenantMnemonic = tenant.mnemonic, "TEST_TENANT-E5597") } returns mockk {
            every { identifier } returns existingIdentifiers
        }
        every { identifierService.getMRNIdentifier(tenant, existingIdentifiers) } returns mockk {
            every { value } returns "MRN"
        }
        every { httpResponse.status } returns HttpStatusCode.OK
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", patientAppointmentSearchUrlPart) }
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns validOldPatientAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                patientAppointmentSearchUrlPart,
                GetPatientAppointmentsRequest(
                    userID = "ehrUserId",
                    startDate = "01/01/2015",
                    endDate = "11/01/2015",
                    patientId = "MRN",
                    patientIdType = tenant.vendorAs<Epic>().patientMRNTypeText
                )
            )
        } returns httpResponse

        val allProviders = validOldPatientAppointmentSearchResponse.appointments.map {
            it.providers
        }.flatten()

        val fakeProvIDs = allProviders.associateWith { prov ->
            val idents = prov.providerIDs.map { Identifier(value = it.id, type = CodeableConcept(text = it.type)) }
            Pair(prov.providerName + "ID", idents)
        }
        fakeProvIDs.entries.forEach {
            it.value
            every { identifierService.getPractitionerIdentifier(tenant, it.value.second) } returns mockk {
                every { value } returns it.value.first
                every { system } returns Uri(tenant.vendorAs<Epic>().practitionerProviderSystem)
            }
        }

        val fakeMap = fakeProvIDs.entries.associate {
            it.key to SystemValue(value = it.value.first, system = tenant.vendorAs<Epic>().practitionerProviderSystem)
        }
        val fakeResults = fakeProvIDs.entries.associate {
            it.key to it.value.first
        }
        every { aidboxPractitionerService.getPractitionerFHIRIds(tenant.mnemonic, fakeMap) } returns fakeResults
        val bundle =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        assertEquals(4, bundle.size)
    }

    @Test
    fun `findPatientAppointments - lookup fails against aidbox throws error`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        val existingIdentifiers = mockk<List<Identifier>> {}
        every { aidboxPatientService.getPatient(tenantMnemonic = tenant.mnemonic, "TEST_TENANT-E5597") } returns mockk {
            every { identifier } returns existingIdentifiers
        }
        every { identifierService.getMRNIdentifier(tenant, existingIdentifiers) } returns mockk {
            every { value } returns "MRN"
        }
        every { httpResponse.status } returns HttpStatusCode.OK
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", patientAppointmentSearchUrlPart) }
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns validOldPatientAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                patientAppointmentSearchUrlPart,
                GetPatientAppointmentsRequest(
                    userID = "ehrUserId",
                    startDate = "01/01/2015",
                    endDate = "11/01/2015",
                    patientId = "MRN",
                    patientIdType = tenant.vendorAs<Epic>().patientMRNTypeText
                )
            )
        } returns httpResponse

        val allProviders = validOldPatientAppointmentSearchResponse.appointments.map {
            it.providers
        }.flatten()

        val fakeProvIDs = allProviders.associateWith { prov ->
            val idents = prov.providerIDs.map { Identifier(value = it.id, type = CodeableConcept(text = it.type)) }
            Pair(prov.providerName + "ID", idents)
        }

        fakeProvIDs.entries.forEach {
            it.value
            every { identifierService.getPractitionerIdentifier(tenant, it.value.second) } returns mockk {
                every { value } returns null
                every { system } returns null
            }
        }

        assertThrows<VendorIdentifierNotFoundException> {
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        }
    }

    @Test
    fun `findPatientAppointmentsByMRN returns patient appointments`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        every { httpResponse.status } returns HttpStatusCode.OK
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", patientAppointmentSearchUrlPart) }
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns validOldPatientAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                patientAppointmentSearchUrlPart,
                GetPatientAppointmentsRequest(
                    userID = "ehrUserId",
                    startDate = "01/01/2015",
                    endDate = "11/01/2015",
                    patientId = "MRN",
                    patientIdType = tenant.vendorAs<Epic>().patientMRNTypeText
                )
            )
        } returns httpResponse

        val allProviders = validOldPatientAppointmentSearchResponse.appointments.map {
            it.providers
        }.flatten()

        val fakeProvIDs = allProviders.associateWith { prov ->
            val idents = prov.providerIDs.map { Identifier(value = it.id, type = CodeableConcept(text = it.type)) }
            Pair(prov.providerName + "ID", idents)
        }
        fakeProvIDs.entries.forEach {
            it.value
            every { identifierService.getPractitionerIdentifier(tenant, it.value.second) } returns mockk {
                every { value } returns it.value.first
                every { system } returns Uri(tenant.vendorAs<Epic>().practitionerProviderSystem)
            }
        }

        val fakeMap = fakeProvIDs.entries.associate {
            it.key to SystemValue(value = it.value.first, system = tenant.vendorAs<Epic>().practitionerProviderSystem)
        }
        val fakeResults = fakeProvIDs.entries.associate {
            it.key to it.value.first
        }
        every { aidboxPractitionerService.getPractitionerFHIRIds(tenant.mnemonic, fakeMap) } returns fakeResults
        val bundle =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "FHIRID",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1),
                "MRN"
            )
        assertEquals(4, bundle.size)
    }

    @Test
    fun `findPatientAppointmentsByMRN no MRN test`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        val fakeID = mockk<Identifier>()
        val fakePat = mockk<Patient> {
            every { identifier } returns listOf(fakeID)
        }
        every { aidboxPatientService.getPatient("TEST_TENANT", "TEST_TENANT-FHIRID") } returns fakePat
        every { identifierService.getMRNIdentifier(tenant, listOf(fakeID)).value } returns null
        val bundle =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "FHIRID",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1),
                null
            )
        assertEquals(0, bundle.size)
    }

    @Test
    fun `findPatientAppointmentsByMRN no MRN no aidbox test`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        val fakeID = mockk<Identifier>()
        val fakePat = mockk<Patient> {
            every { identifier } returns listOf(fakeID)
        }
        every { aidboxPatientService.getPatient("TEST_TENANT", "TEST_TENANT-FHIRID") } throws Exception()
        every { patientService.getPatient(tenant, "FHIRID") } returns fakePat
        every { identifierService.getMRNIdentifier(tenant, listOf(fakeID)).value } returns null
        val bundle =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "FHIRID",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1),
                null
            )
        assertEquals(0, bundle.size)
    }

    @Test
    fun `findProviderAppointments - ensure old API works`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                false
            )
        )

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns validProviderAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments",
                GetProviderAppointmentRequest(
                    userID = "ehrUserId",
                    providers = listOf(ScheduleProvider(id = "E1000")),
                    startDate = "01/01/2015",
                    endDate = "11/01/2015"
                )
            )
        } returns httpResponse

        // Patient service request
        every {
            patientService.getPatientsFHIRIds(
                tenant = tenant,
                patientIDSystem = tenant.vendorAs<Epic>().patientInternalSystem,
                patientIDValues = listOf("     Z6156", "     Z6740", "     Z6783", "     Z4575")
            )
        } returns mapOf(
            "     Z6156" to GetFHIRIDResponse("fhirID1"),
            "     Z6740" to GetFHIRIDResponse("fhirID2"),
            "     Z6783" to GetFHIRIDResponse("fhirID3"),
            "     Z4575" to GetFHIRIDResponse("fhirID4")
        )

        val allProviders = validProviderAppointmentSearchResponse.appointments.map {
            it.providers
        }.flatten()

        val fakeProvIDs = allProviders.associateWith { prov ->
            val idents = prov.providerIDs.map { Identifier(value = it.id, type = CodeableConcept(text = it.type)) }
            Pair(prov.providerName + "ID", idents)
        }
        fakeProvIDs.entries.forEach {
            it.value
            every { identifierService.getPractitionerIdentifier(tenant, it.value.second) } returns mockk {
                every { value } returns it.value.first
                every { system } returns Uri(tenant.vendorAs<Epic>().practitionerProviderSystem)
            }
        }

        val fakeMap = fakeProvIDs.entries.associate {
            it.key to SystemValue(value = it.value.first, system = tenant.vendorAs<Epic>().practitionerProviderSystem)
        }
        val fakeResults = fakeProvIDs.entries.associate {
            it.key to it.value.first
        }
        every { aidboxPractitionerService.getPractitionerFHIRIds(tenant.mnemonic, fakeMap) } returns fakeResults

        val response =
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        assertEquals(6, response.appointments.size)
        assertEquals("38033", response.appointments[0].id!!.value)
        assertEquals("38035", response.appointments[1].id!!.value)
        assertEquals("38034", response.appointments[2].id!!.value)
        assertEquals("38036", response.appointments[3].id!!.value)
        assertEquals("38037", response.appointments[4].id!!.value)
        assertEquals("38184", response.appointments[5].id!!.value)
        assertTrue(response.newPatients!!.isEmpty())
    }

    @Test
    fun `findProviderAppointments - ensure when provider doesn't exist in aidbox we are cool`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                false
            )
        )

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns validProviderAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments",
                GetProviderAppointmentRequest(
                    userID = "ehrUserId",
                    providers = listOf(ScheduleProvider(id = "E1000")),
                    startDate = "01/01/2015",
                    endDate = "11/01/2015"
                )
            )
        } returns httpResponse

        // Patient service request
        every {
            patientService.getPatientsFHIRIds(
                tenant = tenant,
                patientIDSystem = tenant.vendorAs<Epic>().patientInternalSystem,
                patientIDValues = listOf("     Z6156", "     Z6740", "     Z6783", "     Z4575")
            )
        } returns mapOf(
            "     Z6156" to GetFHIRIDResponse("fhirID1"),
            "     Z6740" to GetFHIRIDResponse("fhirID2"),
            "     Z6783" to GetFHIRIDResponse("fhirID3"),
            "     Z4575" to GetFHIRIDResponse("fhirID4")
        )

        val allProviders = validProviderAppointmentSearchResponse.appointments.map {
            it.providers
        }.flatten()

        val fakeProvIDs = allProviders.associateWith { prov ->
            val idents = prov.providerIDs.map { Identifier(value = it.id, type = CodeableConcept(text = it.type)) }
            Pair(prov.providerName + "ID", idents)
        }
        fakeProvIDs.entries.forEach {
            it.value
            every { identifierService.getPractitionerIdentifier(tenant, it.value.second) } returns mockk {
                every { value } returns it.value.first
                every { system } returns Uri(tenant.vendorAs<Epic>().practitionerProviderSystem)
            }
        }

        val fakeMap = fakeProvIDs.entries.associate {
            it.key to SystemValue(value = it.value.first, system = tenant.vendorAs<Epic>().practitionerProviderSystem)
        }
        every { aidboxPractitionerService.getPractitionerFHIRIds(tenant.mnemonic, fakeMap) } returns emptyMap()

        val response = epicAppointmentService.findProviderAppointments(
            tenant,
            listOf(goodProviderFHIRIdentifier),
            LocalDate.of(2015, 1, 1),
            LocalDate.of(2015, 11, 1)
        )

        assertEquals(6, response.appointments.size)
        val providerParticipant =
            response.appointments.first().participant.first { it.actor?.type == Uri("Practitioner") }
        assertNotNull(providerParticipant)
        assertNotNull(providerParticipant.actor?.identifier)
    }

    @Test
    fun `findProviderAppointments - detailed test`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        val epicVendor = tenant.vendorAs<Epic>()

        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxPatientService,
                5,
                false
            )
        )

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }

        val epicAppointment = EpicAppointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "Deliberately new",
            date = "4/30/2015",
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(id = "6789", type = "Internal")
                    ),
                    departmentName = "Test department",
                    duration = "30",
                    providerIDs = listOf(
                        IDType(id = "9876", type = "Internal")
                    ),
                    providerName = "Test Doc",
                    time = "3:30 PM"
                )
            ),
            visitTypeName = "Test visit type",
            contactIDs = listOf(
                IDType(id = "12345", type = "CSN")
            ),
            patientIDs = listOf(
                IDType(id = "54321", type = "Internal")
            )
        )
        val getAppointmentResponse = GetAppointmentsResponse(
            appointments = listOf(epicAppointment),
            error = null
        )

        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns getAppointmentResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments",
                GetProviderAppointmentRequest(
                    userID = "ehrUserId",
                    providers = listOf(ScheduleProvider(id = "E1000")),
                    startDate = "01/01/2015",
                    endDate = "11/01/2015"
                )
            )
        } returns httpResponse

        every {
            patientService.getPatientsFHIRIds(
                tenant,
                epicVendor.patientInternalSystem,
                listOf(epicAppointment.patientId!!)
            )
        } returns mapOf(
            epicAppointment.patientId!! to
                GetFHIRIDResponse("PatientFhirID", null)
        )
        val provider = epicAppointment.providers.first()
        val mockInternalIdentifier =
            Identifier(value = "internal ID", system = Uri(epicVendor.practitionerProviderSystem))
        every {
            identifierService.getPractitionerIdentifier(
                tenant,
                provider.providerIDs.map { Identifier(value = it.id, type = CodeableConcept(text = it.type)) }
            )
        } returns mockInternalIdentifier

        every {
            aidboxPractitionerService.getPractitionerFHIRIds(
                tenant.mnemonic,
                mapOf(provider to SystemValue(mockInternalIdentifier.value!!, mockInternalIdentifier.system!!.value))
            )
        } returns mapOf(provider to "PractitionerFhirID")

        val response =
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        assertEquals(1, response.appointments.size)
        val appt = response.appointments.first()
        assertEquals(epicAppointment.id, appt.id?.value)
        assertEquals("2015-04-30T15:30:00Z", appt.start?.value)
        assertEquals("2015-04-30T16:00:00Z", appt.end?.value)
        assertNull(appt.meta)
        assertEquals(2, appt.identifier.size)
        assertEquals(epicVendor.encounterCSNSystem, appt.identifier[1].system?.value)
        assertEquals(Code("Deliberately new"), appt.status)
        assertEquals(CodeableConcept(text = epicAppointment.visitTypeName), appt.appointmentType)
        val participants = appt.participant
        assertEquals("Patient/PatientFhirID", participants[0].actor?.reference)
        assertEquals("Practitioner/PractitionerFhirID", participants[1].actor?.reference)
    }

    @Test
    fun `all status transformations succeed`() {
        val expectedMappings = mapOf(
            "Arrived" to AppointmentStatus.ARRIVED,
            "Canceled" to AppointmentStatus.CANCELLED,
            "Completed" to AppointmentStatus.FULFILLED,
            "HH incomplete" to AppointmentStatus.CANCELLED,
            "HSP incomplete" to AppointmentStatus.CANCELLED,
            "Left without seen" to AppointmentStatus.NOSHOW,
            "No Show" to AppointmentStatus.NOSHOW,
            "Phoned Patient" to AppointmentStatus.BOOKED,
            "Present" to AppointmentStatus.ARRIVED,
            "Proposed" to AppointmentStatus.PROPOSED,
            "Scheduled" to AppointmentStatus.BOOKED
        )

        val epicAppointmentService = EpicAppointmentService(
            epicClient,
            patientService,
            identifierService,
            aidboxPractitionerService,
            aidboxPatientService,
            5,
            false
        )
        for ((epicStatus, fhirStatus) in expectedMappings) {
            val transformedStatus = epicAppointmentService.transformStatus(epicStatus)
            assertEquals(fhirStatus.code, transformedStatus, "Expected $fhirStatus for $epicStatus")
        }
    }

    @Test
    fun `unknown status does not transform`() {
        val epicAppointmentService = EpicAppointmentService(
            epicClient,
            patientService,
            identifierService,
            aidboxPractitionerService,
            aidboxPatientService,
            5,
            false
        )
        val transformedStatus = epicAppointmentService.transformStatus("This is an Unknown and Unmapped status")
        assertEquals("This is an Unknown and Unmapped status", transformedStatus)
    }
}
