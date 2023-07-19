package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.DocumentReferenceService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EpicDocumentReferenceTest {
    private var epicClient: EpicClient = mockk()
    private var documentReferenceService: DocumentReferenceService = EpicDocumentReferenceService(epicClient)

    @Test
    fun getDocumentReferencesByPatient() {
        val tenant = mockk<Tenant>()

        val documentReference1 = mockk<BundleEntry> {
            every { resource } returns mockk<DocumentReference>(relaxed = true) {
                every { id!!.value } returns "12345"
            }
        }
        val documentReference2 = mockk<BundleEntry> {
            every { resource } returns mockk<DocumentReference>(relaxed = true) {
                every { id!!.value } returns "67890"
            }
        }
        val bundle = mockk<Bundle>(relaxed = true) {
            every { entry } returns listOf(documentReference1, documentReference2)
            every { link } returns emptyList()
        }

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/DocumentReference",
                any()
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "12345")

        val response =
            documentReferenceService.findPatientDocuments(
                tenant,
                "12345",
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2015, 11, 1)
            )

        assertEquals(listOf(documentReference1.resource, documentReference2.resource), response)
    }
}
