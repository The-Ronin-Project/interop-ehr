package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsResponse
import com.projectronin.interop.ehr.epic.client.EpicClient
import io.ktor.client.call.receive
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

class EpicAppointmentServiceTest {
    private lateinit var epicClient: EpicClient
    private lateinit var httpResponse: HttpResponse
    private val validAppointmentSearchResponse =
        readResource<GetPatientAppointmentsResponse>("/ExampleAppointmentBundle.json")
    private val testPrivateKey = this::class.java.getResource("/TestPrivateKey.txt")!!.readText()

    @BeforeEach
    fun initTest() {
        epicClient = mockk<EpicClient>()
        httpResponse = mockk<HttpResponse>()
    }

    @Test
    fun `ensure appointments are returned`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.receive<GetPatientAppointmentsResponse>() } returns validAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments",
                GetPatientAppointmentsRequest("ehrUserId", "1/1/2015", "11/1/2015", "E5597", "MRN")
            )
        } returns httpResponse

        val bundle =
            EpicAppointmentService(epicClient).findAppointments(
                tenant,
                "E5597",
                "1/1/2015",
                "11/1/2015",
            )
        assertEquals(validAppointmentSearchResponse, bundle.resource)
    }

    @Test
    fun `ensure http error handled`() {
        val tenant =
            createTestTenant(
                "d45049c3-3441-40ef-ab4d-b9cd86a17225",
                "https://example.org",
                testPrivateKey,
                "TEST_TENANT"
            )

        every { httpResponse.status } returns HttpStatusCode.NotFound
        coEvery { httpResponse.receive<GetPatientAppointmentsResponse>() } returns validAppointmentSearchResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments",
                GetPatientAppointmentsRequest("ehrUserId", "1/1/2015", "11/1/2015", "E5597", "MRN")
            )
        } returns httpResponse

        assertThrows<IOException> {
            EpicAppointmentService(epicClient).findAppointments(
                tenant,
                "E5597",
                "1/1/2015",
                "11/1/2015",
            )
        }
    }
}
