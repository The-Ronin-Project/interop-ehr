package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.common.http.ktor.throwExceptionFromHttpStatus
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProvider
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.outputs.GetFHIRIDResponse
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.datatype.BundleEntry
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class EpicAppointmentServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var patientService: EpicPatientService
    private lateinit var identifierService: EpicIdentifierService
    private lateinit var httpResponse: HttpResponse
    private val validPatientAppointmentSearchResponse =
        readResource<STU3Bundle>("/ExampleFHIRAppointmentBundle.json")
    private val validProviderAppointmentSearchResponse =
        readResource<GetAppointmentsResponse>("/ExampleProviderAppointmentBundle.json")

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
    private val patientAppointmentSearchUrlPart = "/api/FHIR/STU3/Appointment"
    private val providerAppointmentSearchUrlPart =
        "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments"

    private val singleAppointmentBundle = Bundle(
        id = null,
        entry = listOf(
            BundleEntry(
                resource = Appointment(
                    id = Id("123"),
                    status = AppointmentStatus.BOOKED,
                    participant = listOf()
                )
            )
        ),
        type = BundleType.TRANSACTION_RESPONSE
    )
    private val multipleAppointmentBundle = Bundle(
        id = null,
        entry = listOf(
            BundleEntry(
                resource = Appointment(
                    id = Id("123"),
                    status = AppointmentStatus.BOOKED,
                    participant = listOf()
                )
            ),
            BundleEntry(
                resource = Appointment(
                    id = Id("456"),
                    status = AppointmentStatus.BOOKED,
                    participant = listOf()
                )
            )
        ),
        type = BundleType.TRANSACTION_RESPONSE
    )

    @BeforeEach
    fun initTest() {
        epicClient = mockk()
        httpResponse = mockk()
        patientService = mockk()
        identifierService = mockk()
    }

    @Test
    fun `ensure patient appointments are returned`() {
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
            EpicAppointmentService(epicClient, patientService, identifierService, 5).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        assertEquals(validPatientAppointmentSearchResponse.transformToR4().toListOfType<Appointment>(), bundle)
    }

    @Test
    fun `ensure provider appointments are returned`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(EpicAppointmentService(epicClient, patientService, identifierService, 5))

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetProviderAppointments", providerAppointmentSearchUrlPart) }
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
                patientAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID1",
                    "identifier" to "csnSystem|38033,csnSystem|38035"
                )
            )
        } returns multipleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID2",
                    "identifier" to "csnSystem|38034,csnSystem|38036"
                )
            )
        } returns multipleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID3",
                    "identifier" to "csnSystem|38037"
                )
            )
        } returns singleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientAppointmentSearchUrlPart,
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
    fun `ensure provider appointments returns new patients`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(EpicAppointmentService(epicClient, patientService, identifierService, 5))

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetProviderAppointments", providerAppointmentSearchUrlPart) }
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
                patientAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID1",
                    "identifier" to "csnSystem|38033,csnSystem|38035"
                )
            )
        } returns multipleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID2",
                    "identifier" to "csnSystem|38034,csnSystem|38036"
                )
            )
        } returns multipleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientAppointmentSearchUrlPart,
                mapOf(
                    "patient" to "fhirID3",
                    "identifier" to "csnSystem|38037"
                )
            )
        } returns singleAppointmentBundle
        every {
            epicAppointmentService.getBundleWithPagingSTU3(
                tenant,
                patientAppointmentSearchUrlPart,
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
    fun `ensure provider appointments handles failed GetProviderAppointments call `() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(EpicAppointmentService(epicClient, patientService, identifierService, 5))

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
    fun `ensure provider appointments handles failed identifier service call `() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(EpicAppointmentService(epicClient, patientService, identifierService, 5))

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
    fun `ensure provider appointments handles patient FHIR id not found`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(EpicAppointmentService(epicClient, patientService, identifierService, 5))

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetProviderAppointments", providerAppointmentSearchUrlPart) }
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
    fun `ensure provider appointments handles no appointments found`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        val epicAppointmentService = spyk(EpicAppointmentService(epicClient, patientService, identifierService, 5))

        // Identifier service
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier

        // GetAppointments request
        mockkStatic(HttpResponse::throwExceptionFromHttpStatus)
        justRun { httpResponse.throwExceptionFromHttpStatus("GetProviderAppointments", providerAppointmentSearchUrlPart) }
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns GetAppointmentsResponse(appointments = listOf(), error = null)
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
}
