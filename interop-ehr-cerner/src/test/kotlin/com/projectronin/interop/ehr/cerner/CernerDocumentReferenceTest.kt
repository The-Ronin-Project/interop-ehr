package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import io.ktor.client.call.body
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CernerDocumentReferenceTest {
    private val cernerClient: CernerClient = mockk()

    @Test
    fun getDocumentReferencesByPatient() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                timezone = "UTC-06:00",
            )

        val documentReference1 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<DocumentReference>(relaxed = true) {
                        every { id!!.value } returns "12345"
                    }
            }
        val documentReference2 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<DocumentReference>(relaxed = true) {
                        every { id!!.value } returns "67890"
                    }
            }
        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(documentReference1, documentReference2)
                every { link } returns emptyList()
            }

        coEvery {
            cernerClient.get(
                tenant,
                "/DocumentReference",
                mapOf(
                    "patient" to "12345",
                    "category" to
                        listOf(
                            "LP29684-5",
                            "LP29708-2",
                            "LP75011-4",
                            "LP7819-8",
                            "LP7839-6",
                            "clinical-note",
                        ),
                    "date" to RepeatingParameter(listOf("ge2015-01-01T00:00:00-06:00", "lt2015-11-02T00:00:00-06:00")),
                    "_count" to 20,
                ),
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "12345")

        val documentReferenceService =
            CernerDocumentReferenceService(
                cernerClient,
                "LP29684-5,LP29708-2,LP75011-4,LP7819-8,LP7839-6,clinical-note",
            )
        val response =
            documentReferenceService.findPatientDocuments(
                tenant,
                "12345",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1),
            )

        assertEquals(listOf(documentReference1.resource, documentReference2.resource), response)
    }

    @Test
    fun `getDocumentReferencesByPatient when empty categories sent`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                timezone = "UTC-06:00",
            )

        val documentReference1 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<DocumentReference>(relaxed = true) {
                        every { id!!.value } returns "12345"
                    }
            }
        val documentReference2 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<DocumentReference>(relaxed = true) {
                        every { id!!.value } returns "67890"
                    }
            }
        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(documentReference1, documentReference2)
                every { link } returns emptyList()
            }

        coEvery {
            cernerClient.get(
                tenant,
                "/DocumentReference",
                mapOf(
                    "patient" to "12345",
                    "date" to RepeatingParameter(listOf("ge2015-01-01T00:00:00-06:00", "lt2015-11-02T00:00:00-06:00")),
                    "_count" to 20,
                ),
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "12345")

        val documentReferenceService = CernerDocumentReferenceService(cernerClient, "")
        val response =
            documentReferenceService.findPatientDocuments(
                tenant,
                "12345",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1),
            )

        assertEquals(listOf(documentReference1.resource, documentReference2.resource), response)
    }

    @Test
    fun `getDocumentReferencesByPatient when batch override set`() {
        val tenant =
            createTestTenant(
                clientId = "XhwIjoxNjU0Nzk1NTQ4LCJhenAiOiJEaWNtODQ",
                authEndpoint = "https://example.org",
                secret = "GYtOGM3YS1hNmRmYjc5OWUzYjAiLCJ0Z",
                timezone = "UTC-06:00",
            )

        val documentReference1 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<DocumentReference>(relaxed = true) {
                        every { id!!.value } returns "12345"
                    }
            }
        val documentReference2 =
            mockk<BundleEntry> {
                every { resource } returns
                    mockk<DocumentReference>(relaxed = true) {
                        every { id!!.value } returns "67890"
                    }
            }
        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(documentReference1, documentReference2)
                every { link } returns emptyList()
            }

        coEvery {
            cernerClient.get(
                tenant,
                "/DocumentReference",
                mapOf(
                    "patient" to "12345",
                    "date" to RepeatingParameter(listOf("ge2015-01-01T00:00:00-06:00", "lt2015-11-02T00:00:00-06:00")),
                    "_count" to 2,
                ),
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "12345")

        val documentReferenceService = CernerDocumentReferenceService(cernerClient, "", 2)
        val response =
            documentReferenceService.findPatientDocuments(
                tenant,
                "12345",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1),
            )

        assertEquals(listOf(documentReference1.resource, documentReference2.resource), response)
    }
}
