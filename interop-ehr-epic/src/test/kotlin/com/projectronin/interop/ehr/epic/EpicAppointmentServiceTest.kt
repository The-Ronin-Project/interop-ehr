package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProvider
import com.projectronin.interop.ehr.epic.client.EpicClient
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

class EpicAppointmentServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var httpResponse: HttpResponse
    private val validPatientAppointmentSearchResponse =
        readResource<GetAppointmentsResponse>("/ExampleAppointmentBundle.json")
    private val validProviderAppointmentSearchResponse =
        readResource<GetAppointmentsResponse>("/ExampleProviderAppointmentBundle.json")
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()

    @BeforeEach
    fun initTest() {
        epicClient = mockk()
        httpResponse = mockk()
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
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns validPatientAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments",
                GetPatientAppointmentsRequest("ehrUserId", "01/01/2015", "11/01/2015", "E5597", "MRN")
            )
        } returns httpResponse

        val bundle =
            EpicAppointmentService(epicClient).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        assertEquals(validPatientAppointmentSearchResponse, bundle.resource)
    }

    @Test
    fun `ensure patient http error handled`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.NotFound
        coEvery { httpResponse.body<GetAppointmentsResponse>() } returns validPatientAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments",
                GetPatientAppointmentsRequest("ehrUserId", "01/01/2015", "11/01/2015", "E5597", "MRN")
            )
        } returns httpResponse

        assertThrows<IOException> {
            EpicAppointmentService(epicClient).findPatientAppointments(
                tenant,
                "E5597",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        }
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

        val bundle =
            EpicAppointmentService(epicClient).findProviderAppointments(
                tenant,
                listOf("E1000"),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        assertEquals(validProviderAppointmentSearchResponse, bundle.resource)
    }

    @Test
    fun `ensure provider http error handled`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.NotFound
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

        assertThrows<IOException> {
            EpicAppointmentService(epicClient).findProviderAppointments(
                tenant,
                listOf("E1000"),
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )
        }
    }
}
