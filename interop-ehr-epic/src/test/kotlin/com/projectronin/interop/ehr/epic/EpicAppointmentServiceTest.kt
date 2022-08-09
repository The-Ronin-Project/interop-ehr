package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProvider
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.outputs.GetFHIRIDResponse
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
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
    private val validPatientBundle = readResource<Bundle>("/ExamplePatientBundle.json")

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
            EpicAppointmentService(epicClient, patientService, identifierService).findPatientAppointments(
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
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            )
        } returns goodIdentifier
        every { httpResponse.status } returns HttpStatusCode.OK
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
        every {
            patientService.getPatientFHIRId(
                tenant,
                "     Z6156",
                "internalSystem"
            )
        } returns GetFHIRIDResponse("fhirID")
        every {
            patientService.getPatientFHIRId(
                tenant,
                "     Z6740",
                "internalSystem"
            )
        } returns GetFHIRIDResponse("fhirID2")
        every {
            patientService.getPatientFHIRId(
                tenant,
                "     Z6783",
                "internalSystem"
            )
        } returns GetFHIRIDResponse("fhirID3")
        every {
            patientService.getPatientFHIRId(
                tenant,
                "     Z4575",
                "internalSystem"
            )
        } returns GetFHIRIDResponse("fhirID4", validPatientBundle.toListOfType<Patient>().first())

        val httpResponse2 = mockk<HttpResponse>()
        every { httpResponse2.status } returns HttpStatusCode.OK
        coEvery { httpResponse2.body<STU3Bundle>() } returns validPatientAppointmentSearchResponse
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/STU3/Appointment",
                mapOf(
                    "patient" to "fhirID",
                    "identifier" to "csnSystem|38033"
                )
            )
        } returns httpResponse2
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/STU3/Appointment",
                mapOf(
                    "patient" to "fhirID2",
                    "identifier" to "csnSystem|38034"
                )
            )
        } returns httpResponse2
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/STU3/Appointment",
                mapOf(
                    "patient" to "fhirID",
                    "identifier" to "csnSystem|38035"
                )
            )
        } returns httpResponse2
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/STU3/Appointment",
                mapOf(
                    "patient" to "fhirID2",
                    "identifier" to "csnSystem|38036"
                )
            )
        } returns httpResponse2
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/STU3/Appointment",
                mapOf(
                    "patient" to "fhirID3",
                    "identifier" to "csnSystem|38037"
                )
            )
        } returns httpResponse2
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/STU3/Appointment",
                mapOf(
                    "patient" to "fhirID4",
                    "identifier" to "csnSystem|38184"
                )
            )
        } returns httpResponse2

        val response =
            EpicAppointmentService(epicClient, patientService, identifierService).findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        assertEquals(
            validPatientAppointmentSearchResponse.transformToR4().toListOfType<Appointment>().first(),
            response.appointments.first()
        )
        assertEquals(6, response.appointments.size)
        assertEquals(validPatientBundle.toListOfType<Patient>(), response.newPatients)
    }

    @Test
    fun `ensure provider identifier not found is handled`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )
        every {
            identifierService.getPractitionerProviderIdentifier(
                tenant,
                goodProviderFHIRIdentifier
            ).value
        } returns null

        assertThrows<VendorIdentifierNotFoundException> {
            EpicAppointmentService(epicClient, patientService, identifierService).findProviderAppointments(
                tenant,
                listOf(goodProviderFHIRIdentifier),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        }
    }
}
