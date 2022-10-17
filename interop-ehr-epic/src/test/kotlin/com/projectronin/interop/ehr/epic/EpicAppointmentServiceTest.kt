package com.projectronin.interop.ehr.epic

import com.projectronin.interop.aidbox.LocationService
import com.projectronin.interop.aidbox.PatientService
import com.projectronin.interop.aidbox.PractitionerService
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.common.http.throwExceptionFromHttpStatus
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
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
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
import org.junit.jupiter.api.Assertions.assertFalse
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
    private lateinit var aidboxLocationService: LocationService
    private lateinit var httpResponse: HttpResponse

    private val validPatientAppointmentSearchResponse =
        readResource<STU3Bundle>("/ExampleFHIRAppointmentBundle.json")
    private val validProviderAppointmentSearchResponse =
        readResource<GetAppointmentsResponse>("/ExampleProviderAppointmentBundle.json")
    private val validOldPatientAppointmentSearchResponse =
        readResource<GetAppointmentsResponse>("/ExampleAppointmentBundle.json")
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()
    private val epicProviderSystem = "providerSystem"
    private val goodProviderIdentifier1 = Identifier(
        value = "E1000",
        system = Uri(epicProviderSystem),
        type = CodeableConcept(
            text = "internal"
        )
    )
    private val goodProviderIdentifier2 = Identifier(
        value = "E1000",
        type = CodeableConcept(
            text = "CID"
        )
    )
    private val goodProviderFHIRIdentifier = FHIRIdentifiers(
        id = Id("ProviderFhirId"),
        identifiers = listOf(goodProviderIdentifier1, goodProviderIdentifier2)
    )
    private val badProviderFHIRIdentifier = FHIRIdentifiers(
        id = Id("test2"),
        identifiers = listOf()
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
    private val epicAppointment1 = EpicAppointment(
        appointmentDuration = "30",
        appointmentNotes = listOf("Notes"),
        appointmentStartTime = "3:30 PM",
        appointmentStatus = "Deliberately new",
        date = "4/30/2015",
        patientName = "Test Name",
        providers = listOf(
            ScheduleProviderReturnWithTime(
                departmentIDs = listOf(
                    IDType(id = "6789A", type = "Internal")
                ),
                departmentName = "Test Department A",
                duration = "30",
                providerIDs = listOf(
                    IDType(id = "98761", type = "Internal")
                ),
                providerName = "Test Doc 1",
                time = "3:30 PM"
            ),
            ScheduleProviderReturnWithTime(
                departmentIDs = listOf(
                    IDType(id = "6789B", type = "Internal")
                ),
                departmentName = "Test Department B",
                duration = "30",
                providerIDs = listOf(
                    IDType(id = "98761", type = "Internal")
                ),
                providerName = "Test Doc 1",
                time = "3:30 PM"
            ),
            ScheduleProviderReturnWithTime(
                departmentIDs = listOf(
                    IDType(id = "6789A", type = "Internal")
                ),
                departmentName = "Test Department A",
                duration = "15",
                providerIDs = listOf(
                    IDType(id = "98762", type = "Internal")
                ),
                providerName = "Test Doc 2",
                time = "4:30 PM"
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
    private val epicAppointment2 = EpicAppointment(
        appointmentDuration = "30",
        appointmentNotes = listOf("Notes"),
        appointmentStartTime = "3:30 PM",
        appointmentStatus = "Deliberately unknown",
        date = "4/30/2015",
        patientName = "Test Name 2",
        providers = listOf(
            ScheduleProviderReturnWithTime(
                departmentIDs = listOf(
                    IDType(id = "6789A", type = "Internal")
                ),
                departmentName = "Test Department A",
                duration = "30",
                providerIDs = listOf(
                    IDType(id = "98761", type = "Internal")
                ),
                providerName = "Test Doc 1",
                time = "3:30 PM"
            ),
            ScheduleProviderReturnWithTime(
                departmentIDs = listOf(
                    IDType(id = "6789B", type = "Internal")
                ),
                departmentName = "Test Department B",
                duration = "30",
                providerIDs = listOf(
                    IDType(id = "98763", type = "Internal")
                ),
                providerName = "Test Doc 3",
                time = "3:30 PM"
            ),
            ScheduleProviderReturnWithTime(
                departmentIDs = listOf(
                    IDType(id = "6789D", type = "Internal")
                ),
                departmentName = "Test Department D",
                duration = "15",
                providerIDs = listOf(
                    IDType(id = "98765", type = "Internal")
                ),
                providerName = "Test Doc 5",
                time = "4:30 PM"
            ),
            ScheduleProviderReturnWithTime(
                departmentIDs = listOf(
                    IDType(id = "6789E", type = "Internal")
                ),
                departmentName = "Test Department E",
                duration = "20",
                providerIDs = listOf(
                    IDType(id = "98762", type = "Internal")
                ),
                providerName = "Test Doc 2",
                time = "7:30 PM"
            ),
            ScheduleProviderReturnWithTime(
                departmentIDs = listOf(
                    IDType(id = "6789E", type = "Internal")
                ),
                departmentName = "Test Department E",
                duration = "15",
                providerIDs = listOf(
                    IDType(id = "98767", type = "Internal")
                ),
                providerName = "Test Doc 7",
                time = "8:30 PM"
            ),
            ScheduleProviderReturnWithTime(
                departmentIDs = listOf(
                    IDType(id = "6789B", type = "Internal")
                ),
                departmentName = "Test Department B",
                duration = "45",
                providerIDs = listOf(
                    IDType(id = "98763", type = "Internal")
                ),
                providerName = "Test Doc 3",
                time = "9:30 PM"
            )
        ),
        visitTypeName = "Test visit type",
        contactIDs = listOf(
            IDType(id = "12345", type = "CSN")
        ),
        patientIDs = listOf(
            IDType(id = "543212", type = "Internal")
        )
    )
    private val epicAppointmentList1 = listOf(epicAppointment1, epicAppointment2)

    @BeforeEach
    fun initTest() {
        epicClient = mockk()
        httpResponse = mockk()
        patientService = mockk()
        identifierService = mockk()
        aidboxPractitionerService = mockk()
        aidboxLocationService = mockk()
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
                    "date" to listOf("ge2015-01-01", "le2015-11-01"),
                    "_count" to 50
                )
            )
        } returns httpResponse

        val response =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
                aidboxPatientService,
                5,
                true
            ).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        assertEquals(validPatientAppointmentSearchResponse.transformToR4().toListOfType<Appointment>(), response)
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
                aidboxLocationService,
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
        } returns goodProviderIdentifier1

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
                aidboxLocationService,
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
        } returns goodProviderIdentifier1

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
                aidboxLocationService,
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
        } returns goodProviderIdentifier1

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
    fun `findProviderAppointments- ensure provider appointments handles failed identifier service call for all providers`() {
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
                aidboxLocationService,
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

        val response =
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        assertEquals(0, response.appointments.size)
        assertNull(response.newPatients)
    }

    @Test
    fun `findProviderAppointments - ensure provider appointments handles failed identifier service call for some providers`() {
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
                aidboxLocationService,
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
        } returns goodProviderIdentifier1
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                badProviderFHIRIdentifier
            )
        } returns Identifier(value = null)

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
                listOf(goodProviderFHIRIdentifier, badProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        assertEquals(6, response.appointments.size)
        assertEquals(2, response.newPatients!!.size)
    }

    @Test
    fun `findProviderAppointments - ensure provider appointments handles patient FHIR id not found`() {
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
                aidboxLocationService,
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
        } returns goodProviderIdentifier1

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

        val response =
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        assertEquals(0, response.appointments.size)
        assertEquals(0, response.newPatients!!.size)
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
                aidboxLocationService,
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
        } returns goodProviderIdentifier1

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
                clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                serviceEndpoint = "https://example.org",
                privateKey = testPrivateKey,
                tenantMnemonic = "TEST_TENANT",
                internalId = 1
            )
        val existingIdentifiers = mockk<List<Identifier>> {}

        every {
            aidboxPatientService.getPatient(
                tenantMnemonic = tenant.mnemonic,
                "TEST_TENANT-E5597"
            )
        } returns mockk {
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

        val allProviders = validOldPatientAppointmentSearchResponse.appointments.flatMap { it.providers }
        assertEquals(4, allProviders.size)
        mockEpicProvidersToFhirPractitioners(tenant, allProviders)
        mockEpicDepartmentsToFhirLocations(tenant, allProviders)

        val response =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        assertEquals(4, response.size)
    }

    @Test
    fun `findPatientAppointments - missing Epic provider identifier skips that provider`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        val epicVendor = tenant.vendorAs<Epic>()
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
                    patientIdType = epicVendor.patientMRNTypeText
                )
            )
        } returns httpResponse

        val allProviders = validOldPatientAppointmentSearchResponse.appointments.flatMap { it.providers }
        assertEquals(4, allProviders.size)

        // mock Epic providers with no identifiers
        val mockProvIDs = allProviders.associateWith { prov ->
            prov.providerIDs.map { Identifier(value = it.id, type = CodeableConcept(text = it.type)) }
        }
        mockProvIDs.entries.forEach {
            it.value
            every { identifierService.getPractitionerIdentifier(tenant, it.value) } returns mockk {
                every { value } returns null
                every { system } returns null
            }
        }
        val mockProvSystemValues: Map<ScheduleProviderReturnWithTime, SystemValue> = mockk {
            every { size } returns 0
        }
        every {
            aidboxPractitionerService.getPractitionerFHIRIds(
                tenant.mnemonic,
                mockProvSystemValues
            )
        } returns emptyMap()
        mockEpicDepartmentsToFhirLocations(tenant, allProviders)

        val response =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        // all appointments were found
        assertEquals(4, response.size)

        // no provider identifiers were found, while location identifiers were found
        response.forEach { appt ->
            assertFalse(appt.participant.none { it.actor!!.reference!!.contains("Patient/") })
            assertTrue(appt.participant.none { it.actor!!.reference!!.contains("Practitioner/") })
            assertFalse(appt.participant.none { it.actor!!.reference!!.contains("Location/") })
        }
    }

    @Test
    fun `findPatientAppointments - missing Epic department identifier skips that location`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        val epicVendor = tenant.vendorAs<Epic>()
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
                    patientIdType = epicVendor.patientMRNTypeText
                )
            )
        } returns httpResponse

        val allProviders = validOldPatientAppointmentSearchResponse.appointments.flatMap { it.providers }
        assertEquals(4, allProviders.size)
        mockEpicProvidersToFhirPractitioners(tenant, allProviders)
        val mockLocationIDs = allProviders.associateWith { prov ->
            prov.departmentIDs.map { Identifier(value = it.id, type = CodeableConcept(text = it.type)) }
        }
        mockLocationIDs.entries.forEach {
            it.value
            every { identifierService.getLocationIdentifier(tenant, it.value) } returns mockk {
                every { value } returns null
                every { system } returns null
            }
        }
        val mockLocationSystemValues: Map<ScheduleProviderReturnWithTime, SystemValue> = mockk {
            every { size } returns 0
        }
        every {
            aidboxLocationService.getLocationFHIRIds(
                tenant.mnemonic,
                mockLocationSystemValues
            )
        } returns emptyMap()

        val response =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        // all appointments were found
        assertEquals(4, response.size)

        // no provider identifiers were found, while location identifiers were found
        response.forEach { appt ->
            assertFalse(appt.participant.none { it.actor!!.reference!!.contains("Patient/") })
            assertFalse(appt.participant.none { it.actor!!.reference!!.contains("Practitioner/") })
            assertTrue(appt.participant.none { it.actor!!.reference!!.contains("Location/") })
        }
    }

    @Test
    fun `findPatientAppointments - detailed test - every practitioner and location is unique`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        val epicVendor = tenant.vendorAs<Epic>()
        val existingIdentifiers = mockk<List<Identifier>> {}
        every { aidboxPatientService.getPatient(tenantMnemonic = tenant.mnemonic, "TEST_TENANT-E5597") } returns mockk {
            every { identifier } returns existingIdentifiers
        }
        every { identifierService.getMRNIdentifier(tenant, existingIdentifiers) } returns mockk {
            every { value } returns "MRN"
        }
        every { identifierService.getLocationIdentifier(tenant, existingIdentifiers) } returns mockk {
            every { value } returns "1402684"
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
                    patientIdType = epicVendor.patientMRNTypeText
                )
            )
        } returns httpResponse

        val allProviders = validOldPatientAppointmentSearchResponse.appointments.flatMap { it.providers }
        assertEquals(4, allProviders.size)
        mockEpicProvidersToFhirPractitioners(tenant, allProviders)
        mockEpicDepartmentsToFhirLocations(tenant, allProviders)

        val appointments =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        // all appointments were found
        assertEquals(4, appointments.size)

        // patient, providers, and locations are correct per appointment
        val appt0 = appointments[0]
        assertEquals("22792", appt0.id?.value)
        assertNull(appt0.meta)
        assertEquals(3, appt0.identifier.size)
        assertFalse(appt0.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("noshow"), appt0.status)
        assertEquals(CodeableConcept(text = "TRANSPLANT EVALUATION"), appt0.appointmentType)
        assertEquals(3, appt0.participant.size)
        assertEquals("Patient/E5597", appt0.participant[0].actor?.reference)
        assertEquals("Practitioner/CoordinatorPhoenixRN-fhir-id", appt0.participant[1].actor?.reference)
        assertEquals("Location/EMHPHXCTHDEPT-fhir-id", appt0.participant[2].actor?.reference)
        val appt1 = appointments[1]
        assertEquals("22787", appt1.id?.value)
        assertNull(appt1.meta)
        assertEquals(3, appt1.identifier.size)
        assertFalse(appt1.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("fulfilled"), appt1.status)
        assertEquals(CodeableConcept(text = "INFUSION 120"), appt1.appointmentType)
        assertEquals(3, appt1.participant.size)
        assertEquals("Patient/E5597", appt1.participant[0].actor?.reference)
        assertEquals("Practitioner/INFUSIONSTATION1-fhir-id", appt1.participant[1].actor?.reference)
        assertEquals("Location/EMHINFUSIONTHERAPY-fhir-id", appt1.participant[2].actor?.reference)
        val appt2 = appointments[2]
        assertEquals("22784", appt2.id?.value)
        assertNull(appt2.meta)
        assertEquals(3, appt2.identifier.size)
        assertFalse(appt2.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("booked"), appt2.status)
        assertNull(appt2.appointmentType)
        assertEquals(3, appt2.participant.size)
        assertEquals("Patient/E5597", appt2.participant[0].actor?.reference)
        assertEquals("Practitioner/PEDCHEMOINFUSIONCHAIRA-fhir-id", appt2.participant[1].actor?.reference)
        assertEquals("Location/EMHPEDCHEMOINFUSION-fhir-id", appt2.participant[2].actor?.reference)
        val appt3 = appointments[3]
        assertEquals("22783", appt3.id?.value)
        assertNull(appt3.meta)
        assertEquals(3, appt3.identifier.size)
        assertFalse(appt3.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("arrived"), appt3.status)
        assertEquals(CodeableConcept(text = "OFFICE VISIT"), appt3.appointmentType)
        assertEquals(3, appt3.participant.size)
        assertEquals("Patient/E5597", appt3.participant[0].actor?.reference)
        assertEquals("Practitioner/PhysicianFamilyMedicineMD-fhir-id", appt3.participant[1].actor?.reference)
        assertEquals("Location/EMCFAMILYMEDICINE-fhir-id", appt3.participant[2].actor?.reference)
    }

    @Test
    fun `findPatientAppointments - no practitioners found in aidbox - providers use identifier instead of reference`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        val epicVendor = tenant.vendorAs<Epic>()
        val existingIdentifiers = mockk<List<Identifier>> {}
        every { aidboxPatientService.getPatient(tenantMnemonic = tenant.mnemonic, "TEST_TENANT-E5597") } returns mockk {
            every { identifier } returns existingIdentifiers
        }
        every { identifierService.getMRNIdentifier(tenant, existingIdentifiers) } returns mockk {
            every { value } returns "MRN"
        }
        every { identifierService.getLocationIdentifier(tenant, existingIdentifiers) } returns mockk {
            every { value } returns "1402684"
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
                    patientIdType = epicVendor.patientMRNTypeText
                )
            )
        } returns httpResponse

        val allProviders = validOldPatientAppointmentSearchResponse.appointments.flatMap { it.providers }
        assertEquals(4, allProviders.size)
        mockEpicProvidersToFhirPractitioners(tenant, allProviders, emptyMap())
        mockEpicDepartmentsToFhirLocations(tenant, allProviders)

        val appointments =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        // all appointments were found
        assertEquals(4, appointments.size)

        // patient and locations are correct per appointment, providers use identifier instead of reference
        val appt0 = appointments[0]
        assertEquals("22792", appt0.id?.value)
        assertNull(appt0.meta)
        assertEquals(3, appt0.identifier.size)
        assertFalse(appt0.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("noshow"), appt0.status)
        assertEquals(CodeableConcept(text = "TRANSPLANT EVALUATION"), appt0.appointmentType)
        assertEquals(3, appt0.participant.size)
        assertEquals("Patient/E5597", appt0.participant[0].actor?.reference)
        assertTrue(appt0.participant[1].actor?.identifier is Identifier)
        assertEquals("Location/EMHPHXCTHDEPT-fhir-id", appt0.participant[2].actor?.reference)
        val appt1 = appointments[1]
        assertEquals("22787", appt1.id?.value)
        assertNull(appt1.meta)
        assertEquals(3, appt1.identifier.size)
        assertFalse(appt1.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("fulfilled"), appt1.status)
        assertEquals(CodeableConcept(text = "INFUSION 120"), appt1.appointmentType)
        assertEquals(3, appt1.participant.size)
        assertEquals("Patient/E5597", appt1.participant[0].actor?.reference)
        assertTrue(appt1.participant[1].actor?.identifier is Identifier)
        assertEquals("Location/EMHINFUSIONTHERAPY-fhir-id", appt1.participant[2].actor?.reference)
        val appt2 = appointments[2]
        assertEquals("22784", appt2.id?.value)
        assertNull(appt2.meta)
        assertEquals(3, appt2.identifier.size)
        assertFalse(appt2.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("booked"), appt2.status)
        assertNull(appt2.appointmentType)
        assertEquals(3, appt2.participant.size)
        assertEquals("Patient/E5597", appt2.participant[0].actor?.reference)
        assertTrue(appt2.participant[1].actor?.identifier is Identifier)
        assertEquals("Location/EMHPEDCHEMOINFUSION-fhir-id", appt2.participant[2].actor?.reference)
        val appt3 = appointments[3]
        assertEquals("22783", appt3.id?.value)
        assertNull(appt3.meta)
        assertEquals(3, appt3.identifier.size)
        assertFalse(appt3.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("arrived"), appt3.status)
        assertEquals(CodeableConcept(text = "OFFICE VISIT"), appt3.appointmentType)
        assertEquals(3, appt3.participant.size)
        assertEquals("Patient/E5597", appt3.participant[0].actor?.reference)
        assertTrue(appt3.participant[1].actor?.identifier is Identifier)
        assertEquals("Location/EMCFAMILYMEDICINE-fhir-id", appt3.participant[2].actor?.reference)
    }

    @Test
    fun `findPatientAppointments - no locations found in aidbox - locations are not added to participants`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        val epicVendor = tenant.vendorAs<Epic>()
        val existingIdentifiers = mockk<List<Identifier>> {}
        every { aidboxPatientService.getPatient(tenantMnemonic = tenant.mnemonic, "TEST_TENANT-E5597") } returns mockk {
            every { identifier } returns existingIdentifiers
        }
        every { identifierService.getMRNIdentifier(tenant, existingIdentifiers) } returns mockk {
            every { value } returns "MRN"
        }
        every { identifierService.getLocationIdentifier(tenant, existingIdentifiers) } returns mockk {
            every { value } returns "1402684"
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
                    patientIdType = epicVendor.patientMRNTypeText
                )
            )
        } returns httpResponse

        val allProviders = validOldPatientAppointmentSearchResponse.appointments.flatMap { it.providers }
        assertEquals(4, allProviders.size)
        mockEpicProvidersToFhirPractitioners(tenant, allProviders)
        mockEpicDepartmentsToFhirLocations(tenant, allProviders, emptyMap())

        val appointments =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
                aidboxPatientService,
                5,
                false
            ).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        // all appointments were found
        assertEquals(4, appointments.size)

        // no location identifiers were found, while provider identifiers were found
        appointments.forEach { appt ->
            assertFalse(appt.participant.none { it.actor!!.reference!!.contains("Patient/") })
            assertFalse(appt.participant.none { it.actor!!.reference!!.contains("Practitioner/") })
            assertTrue(appt.participant.none { it.actor!!.reference!!.contains("Location/") })
        }

        // patient and providers are correct per appointment
        assertEquals("22792", appointments[0].id?.value)
        assertEquals(2, appointments[0].participant.size)
        assertEquals("Patient/E5597", appointments[0].participant[0].actor?.reference)
        assertEquals("Practitioner/CoordinatorPhoenixRN-fhir-id", appointments[0].participant[1].actor?.reference)
        assertEquals("Coordinator Phoenix, RN", appointments[0].participant[1].actor?.display)

        assertEquals("22787", appointments[1].id?.value)
        assertEquals(2, appointments[1].participant.size)
        assertEquals("Patient/E5597", appointments[1].participant[0].actor?.reference)
        assertEquals("Practitioner/INFUSIONSTATION1-fhir-id", appointments[1].participant[1].actor?.reference)
        assertEquals("INFUSION STATION 1", appointments[1].participant[1].actor?.display)

        assertEquals("22784", appointments[2].id?.value)
        assertEquals(2, appointments[2].participant.size)
        assertEquals("Patient/E5597", appointments[2].participant[0].actor?.reference)
        assertEquals("Practitioner/PEDCHEMOINFUSIONCHAIRA-fhir-id", appointments[2].participant[1].actor?.reference)
        assertEquals("PED CHEMO INFUSION, CHAIR A", appointments[2].participant[1].actor?.display)

        assertEquals("22783", appointments[3].id?.value)
        assertEquals(2, appointments[3].participant.size)
        assertEquals("Patient/E5597", appointments[2].participant[0].actor?.reference)
        assertEquals("Practitioner/PhysicianFamilyMedicineMD-fhir-id", appointments[3].participant[1].actor?.reference)
        assertEquals("Physician Family Medicine, MD", appointments[3].participant[1].actor?.display)
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

        val allProviders = validOldPatientAppointmentSearchResponse.appointments.flatMap { it.providers }
        assertEquals(4, allProviders.size)
        mockEpicProvidersToFhirPractitioners(tenant, allProviders)
        mockEpicDepartmentsToFhirLocations(tenant, allProviders)

        val response =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
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
        assertEquals(4, response.size)
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
        val mockID = mockk<Identifier>()
        val mockPat = mockk<Patient> {
            every { identifier } returns listOf(mockID)
        }
        every { aidboxPatientService.getPatient("TEST_TENANT", "TEST_TENANT-FHIRID") } returns mockPat
        every { identifierService.getMRNIdentifier(tenant, listOf(mockID)).value } returns null
        val response =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
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
        assertEquals(0, response.size)
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
        val mockID = mockk<Identifier>()
        val mockPat = mockk<Patient> {
            every { identifier } returns listOf(mockID)
        }
        every { aidboxPatientService.getPatient("TEST_TENANT", "TEST_TENANT-FHIRID") } throws Exception()
        every { patientService.getPatient(tenant, "FHIRID") } returns mockPat
        every { identifierService.getMRNIdentifier(tenant, listOf(mockID)).value } returns null
        val response =
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
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
        assertEquals(0, response.size)
    }

    @Test
    fun `findProviderAppointments - ensure old API works`() {
        val tenant =
            createTestTenant(
                clientId = "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                serviceEndpoint = "https://example.org",
                privateKey = testPrivateKey,
                tenantMnemonic = "TEST_TENANT",
                internalId = 1
            )
        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
                aidboxPatientService,
                5,
                false
            )
        )

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                any()
            )
        } returns goodProviderIdentifier1
        every {
            identifierService.getPractitionerIdentifier(
                tenant,
                any()
            )
        } returns goodProviderIdentifier1

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

        val allProviders = validProviderAppointmentSearchResponse.appointments.flatMap { it.providers }
        assertEquals(6, allProviders.size)
        mockEpicProvidersToFhirPractitioners(tenant, allProviders)
        mockEpicDepartmentsToFhirLocations(tenant, allProviders)

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
    fun `findProviderAppointments - detailed test - allows duplicates when same practitioners and locations appear in 1 appointment`() {
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
                aidboxLocationService,
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
        } returns goodProviderIdentifier1

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }

        val getAppointmentResponse = GetAppointmentsResponse(
            appointments = listOf(epicAppointment1),
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
                listOf(epicAppointment1.patientId!!)
            )
        } returns mapOf(
            epicAppointment1.patientId!! to
                GetFHIRIDResponse("PatientFhirID1", null)
        )

        val allProviders = epicAppointment1.providers
        assertEquals(3, allProviders.size)
        mockEpicProvidersToFhirPractitioners(tenant, allProviders)
        mockEpicDepartmentsToFhirLocations(tenant, allProviders)

        val response =
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        assertEquals(1, response.appointments.size)
        val appt = response.appointments[0]
        assertEquals(epicAppointment1.id, appt.id?.value)
        assertNull(appt.meta)
        assertEquals(2, appt.identifier.size)
        assertFalse(appt.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("Deliberately new"), appt.status)
        assertEquals(CodeableConcept(text = epicAppointment1.visitTypeName), appt.appointmentType)
        assertEquals(7, appt.participant.size) // 5 would be the size if we listed only distinct providers
        assertEquals("Patient/PatientFhirID1", appt.participant[0].actor?.reference)
        assertEquals("Test Name", appt.participant[0].actor?.display)
        assertEquals("Practitioner/TestDoc1-fhir-id", appt.participant[1].actor?.reference)
        assertEquals("Test Doc 1", appt.participant[1].actor?.display)
        assertEquals("Practitioner/TestDoc1-fhir-id", appt.participant[2].actor?.reference)
        assertEquals("Test Doc 1", appt.participant[2].actor?.display)
        assertEquals("Practitioner/TestDoc2-fhir-id", appt.participant[3].actor?.reference)
        assertEquals("Test Doc 2", appt.participant[3].actor?.display)
        assertEquals("Location/TestDepartmentA-fhir-id", appt.participant[4].actor?.reference)
        assertEquals("Test Department A", appt.participant[4].actor?.display)
        assertEquals("Location/TestDepartmentB-fhir-id", appt.participant[5].actor?.reference)
        assertEquals("Test Department B", appt.participant[5].actor?.display)
        assertEquals("Location/TestDepartmentA-fhir-id", appt.participant[6].actor?.reference)
        assertEquals("Test Department A", appt.participant[6].actor?.display)
    }

    @Test
    fun `findProviderAppointments - detailed test - allows duplicates when same practitioners and locations appear across a list of appointments`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT",
                timezone = "America/Denver"
            )
        val epicVendor = tenant.vendorAs<Epic>()

        val epicAppointmentService = spyk(
            EpicAppointmentService(
                epicClient,
                patientService,
                identifierService,
                aidboxPractitionerService,
                aidboxLocationService,
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
        } returns goodProviderIdentifier1

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }

        val getAppointmentResponse = GetAppointmentsResponse(
            appointments = epicAppointmentList1,
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
                listOf(epicAppointment1.patientId!!, epicAppointment2.patientId!!)
            )
        } returns mapOf(
            epicAppointment1.patientId!! to
                GetFHIRIDResponse("PatientFhirID1", null),
            epicAppointment2.patientId!! to
                GetFHIRIDResponse("PatientFhirID2", null)

        )

        val allProviders = epicAppointmentList1.flatMap { it.providers }
        assertEquals(9, allProviders.size)
        mockEpicProvidersToFhirPractitioners(tenant, allProviders)
        mockEpicDepartmentsToFhirLocations(tenant, allProviders)

        val response =
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        assertEquals(2, response.appointments.size)
        val appt0 = response.appointments[0]
        assertEquals(epicAppointment1.id, appt0.id?.value)
        assertNull(appt0.meta)
        assertEquals(2, appt0.identifier.size)
        assertFalse(
            appt0.identifier.filter { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } }
                .isEmpty()
        )
        assertEquals(Code("Deliberately new"), appt0.status)
        assertEquals(CodeableConcept(text = epicAppointment1.visitTypeName), appt0.appointmentType)
        assertEquals(7, appt0.participant.size) // 5 would be the size if we listed only distinct providers
        assertEquals("Patient/PatientFhirID1", appt0.participant[0].actor?.reference)
        assertEquals("Test Name", appt0.participant[0].actor?.display)
        assertEquals("Practitioner/TestDoc1-fhir-id", appt0.participant[1].actor?.reference)
        assertEquals("Test Doc 1", appt0.participant[1].actor?.display)
        assertEquals("Practitioner/TestDoc1-fhir-id", appt0.participant[2].actor?.reference)
        assertEquals("Test Doc 1", appt0.participant[2].actor?.display)
        assertEquals("Practitioner/TestDoc2-fhir-id", appt0.participant[3].actor?.reference)
        assertEquals("Test Doc 2", appt0.participant[3].actor?.display)
        assertEquals("Location/TestDepartmentA-fhir-id", appt0.participant[4].actor?.reference)
        assertEquals("Test Department A", appt0.participant[4].actor?.display)
        assertEquals("Location/TestDepartmentB-fhir-id", appt0.participant[5].actor?.reference)
        assertEquals("Test Department B", appt0.participant[5].actor?.display)
        assertEquals("Location/TestDepartmentA-fhir-id", appt0.participant[6].actor?.reference)
        assertEquals("Test Department A", appt0.participant[6].actor?.display)

        val appt1 = response.appointments[1]
        assertEquals(epicAppointment2.id, appt1.id?.value)
        assertNull(appt1.meta)
        assertEquals(2, appt1.identifier.size)
        assertFalse(appt1.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("Deliberately unknown"), appt1.status)
        assertEquals(CodeableConcept(text = epicAppointment1.visitTypeName), appt1.appointmentType)
        assertEquals(13, appt1.participant.size) // 9 would be the size if we listed only distinct providers
        assertEquals("Patient/PatientFhirID2", appt1.participant[0].actor?.reference)
        assertEquals("Test Name 2", appt1.participant[0].actor?.display)
        assertEquals("Practitioner/TestDoc1-fhir-id", appt1.participant[1].actor?.reference)
        assertEquals("Test Doc 1", appt1.participant[1].actor?.display)
        assertEquals("Practitioner/TestDoc3-fhir-id", appt1.participant[2].actor?.reference)
        assertEquals("Test Doc 3", appt1.participant[2].actor?.display)
        assertEquals("Practitioner/TestDoc5-fhir-id", appt1.participant[3].actor?.reference)
        assertEquals("Test Doc 5", appt1.participant[3].actor?.display)
        assertEquals("Practitioner/TestDoc2-fhir-id", appt1.participant[4].actor?.reference)
        assertEquals("Test Doc 2", appt1.participant[4].actor?.display)
        assertEquals("Practitioner/TestDoc7-fhir-id", appt1.participant[5].actor?.reference)
        assertEquals("Test Doc 7", appt1.participant[5].actor?.display)
        assertEquals("Practitioner/TestDoc3-fhir-id", appt1.participant[6].actor?.reference)
        assertEquals("Test Doc 3", appt1.participant[6].actor?.display)
        assertEquals("Location/TestDepartmentA-fhir-id", appt1.participant[7].actor?.reference)
        assertEquals("Test Department A", appt1.participant[7].actor?.display)
        assertEquals("Location/TestDepartmentB-fhir-id", appt1.participant[8].actor?.reference)
        assertEquals("Test Department B", appt1.participant[8].actor?.display)
        assertEquals("Location/TestDepartmentD-fhir-id", appt1.participant[9].actor?.reference)
        assertEquals("Test Department D", appt1.participant[9].actor?.display)
        assertEquals("Location/TestDepartmentE-fhir-id", appt1.participant[10].actor?.reference)
        assertEquals("Test Department E", appt1.participant[10].actor?.display)
        assertEquals("Location/TestDepartmentE-fhir-id", appt1.participant[11].actor?.reference)
        assertEquals("Test Department E", appt1.participant[11].actor?.display)
        assertEquals("Location/TestDepartmentB-fhir-id", appt1.participant[12].actor?.reference)
        assertEquals("Test Department B", appt1.participant[12].actor?.display)
    }

    @Test
    fun `findProviderAppointments - detailed test - correctly assigns unique locations across a list of appointments`() {
        val epicAppointment3 = EpicAppointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "booked",
            date = "4/30/2015",
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(id = "6789A", type = "Internal")
                    ),
                    departmentName = "Test Department A",
                    duration = "30",
                    providerIDs = listOf(
                        IDType(id = "98761", type = "Internal")
                    ),
                    providerName = "Test Doc 1",
                    time = "3:30 PM"
                ),
            ),
            visitTypeName = "Test visit type",
            contactIDs = listOf(
                IDType(id = "12345", type = "CSN")
            ),
            patientIDs = listOf(
                IDType(id = "54321", type = "Internal")
            )
        )
        val epicAppointment4 = EpicAppointment(
            appointmentDuration = "20",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "5:30 PM",
            appointmentStatus = "arrived",
            date = "4/30/2015",
            patientName = "Test Name 2",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(id = "6789B", type = "Internal")
                    ),
                    departmentName = "Test Department B",
                    duration = "20",
                    providerIDs = listOf(
                        IDType(id = "98761", type = "Internal")
                    ),
                    providerName = "Test Doc 1",
                    time = "5:30 PM"
                ),
            ),
            visitTypeName = "Test visit type",
            contactIDs = listOf(
                IDType(id = "12345", type = "CSN")
            ),
            patientIDs = listOf(
                IDType(id = "543212", type = "Internal")
            )
        )
        val epicAppointment5 = EpicAppointment(
            appointmentDuration = "120",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "7:30 PM",
            appointmentStatus = "booked",
            date = "4/30/2015",
            patientName = "Test Name 3",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(id = "6789C", type = "Internal")
                    ),
                    departmentName = "Test Department C",
                    duration = "120",
                    providerIDs = listOf(
                        IDType(id = "98761", type = "Internal")
                    ),
                    providerName = "Test Doc 1",
                    time = "7:30 PM"
                ),
            ),
            visitTypeName = "Test visit type",
            contactIDs = listOf(
                IDType(id = "12345", type = "CSN")
            ),
            patientIDs = listOf(
                IDType(id = "543213", type = "Internal")
            )
        )
        val epicAppointmentList2 = listOf(epicAppointment3, epicAppointment4, epicAppointment5)

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
                aidboxLocationService,
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
        } returns goodProviderIdentifier1

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }

        val getAppointmentResponse = GetAppointmentsResponse(
            appointments = epicAppointmentList2,
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
                listOf(epicAppointment3.patientId!!, epicAppointment4.patientId!!, epicAppointment5.patientId!!)
            )
        } returns mapOf(
            epicAppointment3.patientId!! to
                GetFHIRIDResponse("PatientFhirID1", null),
            epicAppointment4.patientId!! to
                GetFHIRIDResponse("PatientFhirID2", null),
            epicAppointment5.patientId!! to
                GetFHIRIDResponse("PatientFhirID3", null)
        )

        val allProviders = epicAppointmentList2.flatMap { it.providers }
        assertEquals(3, allProviders.size)
        val practitionerFhirIDs = mockEpicProvidersToFhirPractitioners(tenant, allProviders)
        assertEquals(3, practitionerFhirIDs.size)
        val locationFhirIDs = mockEpicDepartmentsToFhirLocations(tenant, allProviders)
        assertEquals(3, locationFhirIDs.size)

        val response =
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        // all appointments found
        assertEquals(3, response.appointments.size)
        // unique Location A (not B or C)
        val appt0 = response.appointments[0]
        assertEquals(epicAppointment1.id, appt0.id?.value)
        assertNull(appt0.meta)
        assertEquals(2, appt0.identifier.size)
        assertFalse(appt0.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("booked"), appt0.status)
        assertEquals(CodeableConcept(text = epicAppointment1.visitTypeName), appt0.appointmentType)
        assertEquals(3, appt0.participant.size)
        assertEquals("Patient/PatientFhirID1", appt0.participant[0].actor?.reference)
        assertEquals("Test Name", appt0.participant[0].actor?.display)
        assertEquals("Practitioner/TestDoc1-fhir-id", appt0.participant[1].actor?.reference)
        assertEquals("Test Doc 1", appt0.participant[1].actor?.display)
        assertEquals("Location/TestDepartmentA-fhir-id", appt0.participant[2].actor?.reference)
        assertEquals("Test Department A", appt0.participant[2].actor?.display)

        // unique Location B (not A or C)
        val appt1 = response.appointments[1]
        assertEquals(epicAppointment2.id, appt1.id?.value)
        assertNull(appt1.meta)
        assertEquals(2, appt1.identifier.size)
        assertFalse(appt1.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("arrived"), appt1.status)
        assertEquals(CodeableConcept(text = epicAppointment1.visitTypeName), appt1.appointmentType)
        assertEquals(3, appt1.participant.size)
        assertEquals("Patient/PatientFhirID2", appt1.participant[0].actor?.reference)
        assertEquals("Test Name 2", appt1.participant[0].actor?.display)
        assertEquals("Practitioner/TestDoc1-fhir-id", appt1.participant[1].actor?.reference)
        assertEquals("Test Doc 1", appt1.participant[1].actor?.display)
        assertEquals("Location/TestDepartmentB-fhir-id", appt1.participant[2].actor?.reference)
        assertEquals("Test Department B", appt1.participant[2].actor?.display)

        // unique Location C (not A or B)
        val appt2 = response.appointments[2]
        assertEquals(epicAppointment2.id, appt2.id?.value)
        assertNull(appt2.meta)
        assertEquals(2, appt2.identifier.size)
        assertFalse(appt2.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("booked"), appt2.status)
        assertEquals(CodeableConcept(text = epicAppointment1.visitTypeName), appt2.appointmentType)
        assertEquals(3, appt2.participant.size)
        assertEquals("Patient/PatientFhirID3", appt2.participant[0].actor?.reference)
        assertEquals("Test Name 3", appt2.participant[0].actor?.display)
        assertEquals("Practitioner/TestDoc1-fhir-id", appt2.participant[1].actor?.reference)
        assertEquals("Test Doc 1", appt2.participant[1].actor?.display)
        assertEquals("Location/TestDepartmentC-fhir-id", appt2.participant[2].actor?.reference)
        assertEquals("Test Department C", appt2.participant[2].actor?.display)
    }

    @Test
    fun `findProviderAppointments - detailed test - correctly assigns unique practitioners across a list of appointments`() {
        val epicAppointment3 = EpicAppointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "3:30 PM",
            appointmentStatus = "booked",
            date = "4/30/2015",
            patientName = "Test Name",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(id = "6789A", type = "Internal")
                    ),
                    departmentName = "Test Department A",
                    duration = "30",
                    providerIDs = listOf(
                        IDType(id = "98762", type = "Internal")
                    ),
                    providerName = "Test Doc 2",
                    time = "3:30 PM"
                ),
            ),
            visitTypeName = "Test visit type",
            contactIDs = listOf(
                IDType(id = "12345", type = "CSN")
            ),
            patientIDs = listOf(
                IDType(id = "54321", type = "Internal")
            )
        )
        val epicAppointment4 = EpicAppointment(
            appointmentDuration = "20",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "5:30 PM",
            appointmentStatus = "arrived",
            date = "4/30/2015",
            patientName = "Test Name 2",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(id = "6789A", type = "Internal")
                    ),
                    departmentName = "Test Department A",
                    duration = "20",
                    providerIDs = listOf(
                        IDType(id = "98761", type = "Internal")
                    ),
                    providerName = "Test Doc 1",
                    time = "5:30 PM"
                ),
            ),
            visitTypeName = "Test visit type",
            contactIDs = listOf(
                IDType(id = "12345", type = "CSN")
            ),
            patientIDs = listOf(
                IDType(id = "543212", type = "Internal")
            )
        )
        val epicAppointment5 = EpicAppointment(
            appointmentDuration = "120",
            appointmentNotes = listOf("Notes"),
            appointmentStartTime = "7:30 PM",
            appointmentStatus = "booked",
            date = "4/30/2015",
            patientName = "Test Name 3",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(id = "6789A", type = "Internal")
                    ),
                    departmentName = "Test Department A",
                    duration = "120",
                    providerIDs = listOf(
                        IDType(id = "98763", type = "Internal")
                    ),
                    providerName = "Test Doc 3",
                    time = "7:30 PM"
                ),
            ),
            visitTypeName = "Test visit type",
            contactIDs = listOf(
                IDType(id = "12345", type = "CSN")
            ),
            patientIDs = listOf(
                IDType(id = "543213", type = "Internal")
            )
        )
        val epicAppointmentList2 = listOf(epicAppointment3, epicAppointment4, epicAppointment5)

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
                aidboxLocationService,
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
        } returns goodProviderIdentifier1

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetAppointments", providerAppointmentSearchUrlPart) }

        val getAppointmentResponse = GetAppointmentsResponse(
            appointments = epicAppointmentList2,
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
                listOf(epicAppointment3.patientId!!, epicAppointment4.patientId!!, epicAppointment5.patientId!!)
            )
        } returns mapOf(
            epicAppointment3.patientId!! to
                GetFHIRIDResponse("PatientFhirID1", null),
            epicAppointment4.patientId!! to
                GetFHIRIDResponse("PatientFhirID2", null),
            epicAppointment5.patientId!! to
                GetFHIRIDResponse("PatientFhirID3", null)
        )

        val allProviders = epicAppointmentList2.flatMap { it.providers }
        assertEquals(3, allProviders.size)
        val practitionerFhirIDs = mockEpicProvidersToFhirPractitioners(tenant, allProviders)
        assertEquals(3, practitionerFhirIDs.size)
        val locationFhirIDs = mockEpicDepartmentsToFhirLocations(tenant, allProviders)
        assertEquals(1, locationFhirIDs.size)

        val response =
            epicAppointmentService.findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        // all appointments found
        assertEquals(3, response.appointments.size)
        // unique Practitioner 2 (not 1 or 3)
        val appt0 = response.appointments[0]
        assertEquals(epicAppointment1.id, appt0.id?.value)
        assertNull(appt0.meta)
        assertEquals(2, appt0.identifier.size)
        assertFalse(appt0.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("booked"), appt0.status)
        assertEquals(CodeableConcept(text = epicAppointment1.visitTypeName), appt0.appointmentType)
        assertEquals(3, appt0.participant.size)
        assertEquals("Patient/PatientFhirID1", appt0.participant[0].actor?.reference)
        assertEquals("Test Name", appt0.participant[0].actor?.display)
        assertEquals("Practitioner/TestDoc2-fhir-id", appt0.participant[1].actor?.reference)
        assertEquals("Test Doc 2", appt0.participant[1].actor?.display)
        assertEquals("Location/TestDepartmentA-fhir-id", appt0.participant[2].actor?.reference)
        assertEquals("Test Department A", appt0.participant[2].actor?.display)

        // unique Practitioner 1 (not 2 or 3)
        val appt1 = response.appointments[1]
        assertEquals(epicAppointment2.id, appt1.id?.value)
        assertNull(appt1.meta)
        assertEquals(2, appt1.identifier.size)
        assertFalse(appt1.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("arrived"), appt1.status)
        assertEquals(CodeableConcept(text = epicAppointment1.visitTypeName), appt1.appointmentType)
        assertEquals(3, appt1.participant.size)
        assertEquals("Patient/PatientFhirID2", appt1.participant[0].actor?.reference)
        assertEquals("Test Name 2", appt1.participant[0].actor?.display)
        assertEquals("Practitioner/TestDoc1-fhir-id", appt1.participant[1].actor?.reference)
        assertEquals("Test Doc 1", appt1.participant[1].actor?.display)
        assertEquals("Location/TestDepartmentA-fhir-id", appt0.participant[2].actor?.reference)
        assertEquals("Test Department A", appt0.participant[2].actor?.display)

        // unique Practitioner 3 (not 1 or 2)
        val appt2 = response.appointments[2]
        assertEquals(epicAppointment2.id, appt2.id?.value)
        assertNull(appt2.meta)
        assertEquals(2, appt2.identifier.size)
        assertFalse(appt2.identifier.none { ident -> ident.system.let { it?.value == epicVendor.encounterCSNSystem } })
        assertEquals(Code("booked"), appt2.status)
        assertEquals(CodeableConcept(text = epicAppointment1.visitTypeName), appt2.appointmentType)
        assertEquals(3, appt2.participant.size)
        assertEquals("Patient/PatientFhirID3", appt2.participant[0].actor?.reference)
        assertEquals("Test Name 3", appt2.participant[0].actor?.display)
        assertEquals("Practitioner/TestDoc3-fhir-id", appt2.participant[1].actor?.reference)
        assertEquals("Test Doc 3", appt2.participant[1].actor?.display)
        assertEquals("Location/TestDepartmentA-fhir-id", appt0.participant[2].actor?.reference)
        assertEquals("Test Department A", appt0.participant[2].actor?.display)
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
            aidboxLocationService,
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
            aidboxLocationService,
            aidboxPatientService,
            5,
            false
        )
        val transformedStatus = epicAppointmentService.transformStatus("This is an Unknown and Unmapped status")
        assertEquals("This is an Unknown and Unmapped status", transformedStatus)
    }

    /**
     * Provide all necessary mocking answers for getPractitionerIdentifier() and getPractitionerFhirIDs().
     * @param fhirIDsMockResult if provided is the return for getLocationFhirIDs(), otherwise mock that return here.
     * @return the mocked return from getLocationFhirIDs(). This value can be checked for size or to verify mocking.
     */
    private fun mockEpicProvidersToFhirPractitioners(
        tenant: Tenant,
        allProviders: List<ScheduleProviderReturnWithTime>,
        fhirIDsMockResult: Map<ScheduleProviderReturnWithTime, String>? = null
    ): Map<ScheduleProviderReturnWithTime, String> {
        val epicVendor = tenant.vendorAs<Epic>()
        val mockProviderIDs = allProviders.associateWith { participant ->
            participant.providerIDs.map { Identifier(value = it.id, type = CodeableConcept(text = it.type)) }
        }
        val mockProviderQuery = mutableMapOf<SystemValue, SystemValue>()
        val mockPractitionerFhirIDsResult = mutableMapOf<SystemValue, String>()
        val mockPractitionerFhirIDs = mutableMapOf<ScheduleProviderReturnWithTime, String>()
        mockProviderIDs.entries.forEach { mapEntry ->
            every { identifierService.getPractitionerIdentifier(tenant, mapEntry.value) } returns mockk {
                every { value } returns mapEntry.value.first().value
                every { system } returns Uri(epicVendor.practitionerProviderSystem)
            }
            val mockSystemValue = SystemValue(mapEntry.value.first().value!!, epicVendor.practitionerProviderSystem)
            if (!mockProviderQuery.keys.contains(mockSystemValue)) {
                mockProviderQuery[mockSystemValue] = mockSystemValue
            }
            if (fhirIDsMockResult == null) {
                val mockFhirID = "${mapEntry.key.providerName.replace("[^a-zA-Z0-9]".toRegex(), "")}-fhir-id"
                mockPractitionerFhirIDsResult[mockSystemValue] = mockFhirID
                mockPractitionerFhirIDs[mapEntry.key] = mockFhirID
            }
        }
        val fhirIDsReturn = fhirIDsMockResult ?: mockPractitionerFhirIDs
        every {
            aidboxPractitionerService.getPractitionerFHIRIds(
                tenant.mnemonic,
                mockProviderQuery
            )
        } returns mockPractitionerFhirIDsResult
        return fhirIDsReturn
    }

    /**
     * Provide all necessary mocking answers for getLocationIdentifier() and getLocationFhirIDs().
     * @param fhirIDsMockResult if provided is the return for getLocationFhirIDs(), otherwise mock that return here.
     * @return the mocked return from getLocationFhirIDs(). This value can be checked for size or to verify mocking.
     */
    private fun mockEpicDepartmentsToFhirLocations(
        tenant: Tenant,
        allProviders: List<ScheduleProviderReturnWithTime>,
        fhirIDsMockResult: Map<ScheduleProviderReturnWithTime, String>? = null
    ): Map<ScheduleProviderReturnWithTime, String> {
        val epicVendor = tenant.vendorAs<Epic>()
        val mockDepartmentIDs = allProviders.associateWith { participant ->
            participant.departmentIDs.map { Identifier(value = it.id, type = CodeableConcept(text = it.type)) }
        }
        val mockLocationQuery = mutableMapOf<SystemValue, SystemValue>()
        val mockLocationFhirIDsResult = mutableMapOf<SystemValue, String>()
        val mockLocationFhirIDs = mutableMapOf<ScheduleProviderReturnWithTime, String>()
        mockDepartmentIDs.entries.forEach { mapEntry ->
            every { identifierService.getLocationIdentifier(tenant, mapEntry.value) } returns mockk {
                every { value } returns mapEntry.value.first().value
                every { system } returns Uri(epicVendor.departmentInternalSystem)
            }
            val mockSystemValue = SystemValue(mapEntry.value.first().value!!, epicVendor.departmentInternalSystem)
            if (!mockLocationQuery.keys.contains(mockSystemValue)) {
                mockLocationQuery[mockSystemValue] = mockSystemValue
                if (fhirIDsMockResult == null) {
                    val mockFhirID = "${mapEntry.key.departmentName.replace("[^a-zA-Z0-9]".toRegex(), "")}-fhir-id"
                    mockLocationFhirIDsResult[mockSystemValue] = mockFhirID
                    mockLocationFhirIDs[mapEntry.key] = mockFhirID
                }
            }
        }
        val fhirIDsReturn = fhirIDsMockResult ?: mockLocationFhirIDs
        every {
            aidboxLocationService.getLocationFHIRIds(
                tenant.mnemonic,
                mockLocationQuery
            )
        } returns mockLocationFhirIDsResult
        return fhirIDsReturn
    }
}
