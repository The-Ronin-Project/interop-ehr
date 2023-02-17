package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.aidbox.PatientService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.cerner.client.RepeatingParameter
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class CernerAppointmentServiceTest {
    private lateinit var cernerClient: CernerClient
    private lateinit var aidboxPatientService: PatientService
    private lateinit var cernerPatientService: CernerPatientService
    private lateinit var appointmentService: CernerAppointmentService
    private lateinit var httpResponse: HttpResponse
    private lateinit var tenant: Tenant
    private val validAppointmentBundle = readResource<Bundle>("/ExampleAppointmentBundle.json")

    @BeforeEach
    fun initTest() {
        cernerClient = mockk()
        aidboxPatientService = mockk()
        httpResponse = mockk()
        cernerPatientService = mockk()
        appointmentService = CernerAppointmentService(cernerClient, aidboxPatientService, cernerPatientService)
        tenant = createTestTenant(
            clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
            authEndpoint = "https://example.org",
            secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z"
        )
    }

    @Test
    fun `findPatientAppointments - works`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validAppointmentBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Appointment",
                mapOf(
                    "patient" to "patientFhirId",
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00Z", "lt2023-01-08T00:00:00Z")),
                    "_count" to 20
                )
            )
        } returns httpResponse
        val appointments = appointmentService.findPatientAppointments(
            tenant = tenant,
            patientFHIRId = "patientFhirId",
            startDate = LocalDate.of(2023, 1, 3),
            endDate = LocalDate.of(2023, 1, 7)
        )
        assertEquals(1, appointments.size)
    }

    @Test
    fun `findProviderAppointments - works, no new patients`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validAppointmentBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Appointment",
                mapOf(
                    "practitioner" to "prac123,prac345",
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00Z", "lt2023-01-08T00:00:00Z")),
                    "_count" to 20
                )
            )
        } returns httpResponse
        every { aidboxPatientService.getPatientFHIRIds<String>(tenant.mnemonic, any()) } returns mapOf("12724066" to "12724066")
        val response = appointmentService.findProviderAppointments(
            tenant = tenant,
            providerIDs = listOf(
                FHIRIdentifiers(id = Id("prac123"), identifiers = emptyList()),
                FHIRIdentifiers(id = Id("prac345"), identifiers = emptyList()),
            ),
            startDate = LocalDate.of(2023, 1, 3),
            endDate = LocalDate.of(2023, 1, 7)
        )
        assertEquals(1, response.appointments.size)
        assertEquals(0, response.newPatients?.size)
    }

    @Test
    fun `findProviderAppointments - works, with new patients`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validAppointmentBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Appointment",
                mapOf(
                    "practitioner" to "prac123,prac345",
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00Z", "lt2023-01-08T00:00:00Z")),
                    "_count" to 20
                )
            )
        } returns httpResponse
        every { aidboxPatientService.getPatientFHIRIds<String>(tenant.mnemonic, any()) } returns emptyMap()
        every { cernerPatientService.getPatient(tenant, "12724066") } returns mockk()
        val response = appointmentService.findProviderAppointments(
            tenant = tenant,
            providerIDs = listOf(
                FHIRIdentifiers(id = Id("prac123"), identifiers = emptyList()),
                FHIRIdentifiers(id = Id("prac345"), identifiers = emptyList()),
            ),
            startDate = LocalDate.of(2023, 1, 3),
            endDate = LocalDate.of(2023, 1, 7)
        )
        assertEquals(1, response.appointments.size)
        assertEquals(1, response.newPatients?.size)
    }

    @Test
    fun `findLocationAppointments - works, no new patients`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns validAppointmentBundle
        coEvery {
            cernerClient.get(
                tenant,
                "/Appointment",
                mapOf(
                    "location" to "loc123,loc345",
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00Z", "lt2023-01-08T00:00:00Z")),
                    "_count" to 20
                )
            )
        } returns httpResponse
        every { aidboxPatientService.getPatientFHIRIds<String>(tenant.mnemonic, any()) } returns mapOf("12724066" to "12724066")
        val response = appointmentService.findLocationAppointments(
            tenant = tenant,
            locationFHIRIds = listOf("loc123", "loc345"),
            startDate = LocalDate.of(2023, 1, 3),
            endDate = LocalDate.of(2023, 1, 7)
        )
        assertEquals(1, response.appointments.size)
        assertEquals(0, response.newPatients?.size)
    }

    @Test
    fun `findLocationAppointments - throws error when missing valid patient reference`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns mockk {
            every { link } returns emptyList()
            every { entry } returns listOf(
                mockk {
                    every { resource } returns mockk<Appointment> {
                        every { participant } returns listOf(
                            // this one is causing an error
                            mockk {
                                every { type } returns listOf(mockk { every { text?.value } returns "Patient" })
                                every { actor } returns null
                            },
                        )
                    }
                }
            )
        }
        coEvery {
            cernerClient.get(
                tenant,
                "/Appointment",
                mapOf(
                    "location" to "loc123,loc345",
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00Z", "lt2023-01-08T00:00:00Z")),
                    "_count" to 20
                )
            )
        } returns httpResponse
        every { aidboxPatientService.getPatientFHIRIds<String>(tenant.mnemonic, any()) } returns mapOf("12724066" to "12724066")
        assertThrows<NullPointerException> {
            appointmentService.findLocationAppointments(
                tenant = tenant,
                locationFHIRIds = listOf("loc123", "loc345"),
                startDate = LocalDate.of(2023, 1, 3),
                endDate = LocalDate.of(2023, 1, 7)
            )
        }
        // do it again but make the reference slightly different
        coEvery { httpResponse.body<Bundle>() } returns mockk {
            every { link } returns emptyList()
            every { entry } returns listOf(
                mockk {
                    every { resource } returns mockk<Appointment> {
                        every { participant } returns listOf(
                            // this one is causing an error
                            mockk {
                                every { type } returns listOf(mockk { every { text?.value } returns "Patient" })
                                every { actor?.reference } returns null
                            },
                        )
                    }
                }
            )
        }
        assertThrows<NullPointerException> {
            appointmentService.findLocationAppointments(
                tenant = tenant,
                locationFHIRIds = listOf("loc123", "loc345"),
                startDate = LocalDate.of(2023, 1, 3),
                endDate = LocalDate.of(2023, 1, 7)
            )
        }
    }

    @Test
    fun `isPatient - works`() {
        val participant = mockk<Participant> {
            every { actor } returns null
            every { type } returns emptyList()
        }
        assertFalse(participant.isPatient())

        every { participant.type } returns listOf(mockk { every { text } returns null })
        assertFalse(participant.isPatient())
        every { participant.type } returns listOf(mockk { every { text?.value } returns "NotPatient" })
        assertFalse(participant.isPatient())
        every { participant.type } returns listOf(mockk { every { text?.value } returns "Patient" })
        assertTrue(participant.isPatient())

        every { participant.type } returns emptyList()
        every { participant.actor?.reference } returns null
        assertFalse(participant.isPatient())
        every { participant.actor?.reference?.value } returns null
        assertFalse(participant.isPatient())
        every { participant.actor?.reference?.value } returns "NotPatient"
        assertFalse(participant.isPatient())
        every { participant.actor?.reference?.value } returns "Patient"
        assertTrue(participant.isPatient())
    }
}
