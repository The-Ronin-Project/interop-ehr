package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Medication
import io.ktor.client.statement.HttpResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CernerMedicationServiceTest {
    private val client = mockk<CernerClient>()
    private val service = CernerMedicationService(client)

    @Test
    fun `get function works`() {
        val medication = mockk<Medication>(relaxed = true) {
        }
        val response = mockk<HttpResponse> {
            every { call } returns mockk {
                coEvery { bodyNullable(any()) } returns mockk<Bundle>(relaxed = true) {
                    every { link } returns emptyList()
                    every { entry } returns listOf(
                        mockk {
                            every { resource } returns medication
                        }
                    )
                }
            }
        }
        coEvery { client.get(any(), any(), any()) } returns EHRResponse(response, "12345")
        assertEquals(service.getMedicationsByFhirId(mockk(), listOf("123")), listOf(medication))
    }
}
