package com.projectronin.interop.ehr.cerner

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.ehr.dataauthority.models.IdentifierSearchResponse
import com.projectronin.ehr.dataauthority.models.IdentifierSearchableResourceTypes
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class CernerAppointmentServiceTest {
    private lateinit var cernerClient: CernerClient
    private lateinit var ehrDataAuthorityClient: EHRDataAuthorityClient
    private lateinit var cernerPatientService: CernerPatientService
    private lateinit var appointmentService: CernerAppointmentService
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private lateinit var tenant: Tenant
    private val validAppointmentBundle = readResource<Bundle>("/ExampleAppointmentBundle.json")

    @BeforeEach
    fun initTest() {
        cernerClient = mockk()
        ehrDataAuthorityClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
        cernerPatientService = mockk()
        appointmentService = CernerAppointmentService(cernerClient, ehrDataAuthorityClient, cernerPatientService)
        tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                timezone = "UTC-06:00",
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
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00-06:00", "lt2023-01-08T00:00:00-06:00")),
                    "_count" to 20,
                ),
            )
        } returns ehrResponse
        val appointments =
            appointmentService.findPatientAppointments(
                tenant = tenant,
                patientFHIRId = "patientFhirId",
                startDate = LocalDate.of(2023, 1, 3),
                endDate = LocalDate.of(2023, 1, 7),
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
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00-06:00", "lt2023-01-08T00:00:00-06:00")),
                    "_count" to 20,
                ),
            )
        } returns ehrResponse
        val mockResponse =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier.value } returns "12724066"
                    every { foundResources } returns listOf(mockk())
                },
            )
        coEvery {
            ehrDataAuthorityClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                any(),
            )
        } returns mockResponse
        val response =
            appointmentService.findProviderAppointments(
                tenant = tenant,
                providerIDs =
                    listOf(
                        FHIRIdentifiers(id = Id("prac123"), identifiers = emptyList()),
                        FHIRIdentifiers(id = Id("prac345"), identifiers = emptyList()),
                    ),
                startDate = LocalDate.of(2023, 1, 3),
                endDate = LocalDate.of(2023, 1, 7),
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
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00-06:00", "lt2023-01-08T00:00:00-06:00")),
                    "_count" to 20,
                ),
            )
        } returns ehrResponse
        coEvery {
            ehrDataAuthorityClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                any(),
            )
        } returns emptyList()
        every { cernerPatientService.getPatient(tenant, "12724066") } returns mockk()
        val response =
            appointmentService.findProviderAppointments(
                tenant = tenant,
                providerIDs =
                    listOf(
                        FHIRIdentifiers(id = Id("prac123"), identifiers = emptyList()),
                        FHIRIdentifiers(id = Id("prac345"), identifiers = emptyList()),
                    ),
                startDate = LocalDate.of(2023, 1, 3),
                endDate = LocalDate.of(2023, 1, 7),
            )
        assertEquals(1, response.appointments.size)
        assertEquals(1, response.newPatients?.size)
    }

    @Test
    fun `findProviderAppointments - works when no appointments found`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns
            mockk(relaxed = true) {
                every { entry } returns emptyList()
            }
        coEvery {
            cernerClient.get(
                tenant,
                "/Appointment",
                mapOf(
                    "practitioner" to "prac123,prac345",
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00-06:00", "lt2023-01-08T00:00:00-06:00")),
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val response =
            appointmentService.findProviderAppointments(
                tenant = tenant,
                providerIDs =
                    listOf(
                        FHIRIdentifiers(id = Id("prac123"), identifiers = emptyList()),
                        FHIRIdentifiers(id = Id("prac345"), identifiers = emptyList()),
                    ),
                startDate = LocalDate.of(2023, 1, 3),
                endDate = LocalDate.of(2023, 1, 7),
            )
        assertEquals(0, response.appointments.size)
        assertEquals(0, response.newPatients?.size)

        verify { ehrDataAuthorityClient wasNot Called }
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
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00-06:00", "lt2023-01-08T00:00:00-06:00")),
                    "_count" to 20,
                ),
            )
        } returns ehrResponse
        val mockResponse =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier.value } returns "12724066"
                    every { foundResources } returns listOf(mockk())
                },
            )
        coEvery {
            ehrDataAuthorityClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                any(),
            )
        } returns mockResponse
        val response =
            appointmentService.findLocationAppointments(
                tenant = tenant,
                locationFHIRIds = listOf("loc123", "loc345"),
                startDate = LocalDate.of(2023, 1, 3),
                endDate = LocalDate.of(2023, 1, 7),
            )
        assertEquals(1, response.appointments.size)
        assertEquals(0, response.newPatients?.size)
    }

    @Test
    fun `findLocationAppointments - throws error when missing valid patient reference`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns
            mockk(relaxed = true) {
                every { link } returns emptyList()
                every { entry } returns
                    listOf(
                        mockk {
                            every { resource } returns
                                mockk<Appointment>(relaxed = true) {
                                    every { participant } returns
                                        listOf(
                                            // this one is causing an error
                                            mockk {
                                                every { type } returns listOf(mockk { every { text?.value } returns "Patient" })
                                                every { actor } returns null
                                            },
                                        )
                                }
                        },
                    )
            }
        coEvery {
            cernerClient.get(
                tenant,
                "/Appointment",
                mapOf(
                    "location" to "loc123,loc345",
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00-06:00", "lt2023-01-08T00:00:00-06:00")),
                    "_count" to 20,
                ),
            )
        } returns ehrResponse
        val mockResponse =
            listOf<IdentifierSearchResponse>(
                mockk {
                    every { searchedIdentifier.value } returns "12724066"
                    every { foundResources } returns listOf(mockk())
                },
            )
        coEvery {
            ehrDataAuthorityClient.getResourceIdentifiers(
                tenant.mnemonic,
                IdentifierSearchableResourceTypes.Patient,
                any(),
            )
        } returns mockResponse
        assertThrows<NullPointerException> {
            appointmentService.findLocationAppointments(
                tenant = tenant,
                locationFHIRIds = listOf("loc123", "loc345"),
                startDate = LocalDate.of(2023, 1, 3),
                endDate = LocalDate.of(2023, 1, 7),
            )
        }
        // do it again but make the reference slightly different
        coEvery { httpResponse.body<Bundle>() } returns
            mockk(relaxed = true) {
                every { link } returns emptyList()
                every { entry } returns
                    listOf(
                        mockk {
                            every { resource } returns
                                mockk<Appointment>(relaxed = true) {
                                    every { participant } returns
                                        listOf(
                                            // this one is causing an error
                                            mockk {
                                                every { type } returns listOf(mockk { every { text?.value } returns "Patient" })
                                                every { actor?.reference } returns null
                                            },
                                        )
                                }
                        },
                    )
            }
        assertThrows<NullPointerException> {
            appointmentService.findLocationAppointments(
                tenant = tenant,
                locationFHIRIds = listOf("loc123", "loc345"),
                startDate = LocalDate.of(2023, 1, 3),
                endDate = LocalDate.of(2023, 1, 7),
            )
        }
    }

    @Test
    fun `findLocationAppointments - works when no appointments found`() {
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<Bundle>() } returns
            mockk(relaxed = true) {
                every { entry } returns emptyList()
            }
        coEvery {
            cernerClient.get(
                tenant,
                "/Appointment",
                mapOf(
                    "location" to "loc123,loc345",
                    "date" to RepeatingParameter(listOf("ge2023-01-03T00:00:00-06:00", "lt2023-01-08T00:00:00-06:00")),
                    "_count" to 20,
                ),
            )
        } returns ehrResponse

        val response =
            appointmentService.findLocationAppointments(
                tenant = tenant,
                locationFHIRIds = listOf("loc123", "loc345"),
                startDate = LocalDate.of(2023, 1, 3),
                endDate = LocalDate.of(2023, 1, 7),
            )
        assertEquals(0, response.appointments.size)
        assertEquals(0, response.newPatients?.size)

        verify { ehrDataAuthorityClient wasNot Called }
    }

    @Test
    fun `isPatient - works`() {
        val participant =
            mockk<Participant> {
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
