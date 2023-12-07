package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

internal class CernerMedicationAdministrationServiceTest {
    private val client: CernerClient = mockk()
    private val service = CernerMedicationAdministrationService(client)
    private val testTenant: Tenant =
        mockk {
            every { mnemonic } returns "testTenant"
            every { timezone } returns ZoneId.of("Etc/UTC")
        }

    @Test
    fun `happy path`() {
        val medAdmin1 =
            mockk<BundleEntry>(relaxed = true) {
                every { resource } returns
                    mockk<MedicationAdministration>(relaxed = true) {
                        every { id } returns Id("medAdmin1")
                    }
            }
        val medAdmin2 =
            mockk<BundleEntry>(relaxed = true) {
                every { resource } returns
                    mockk<MedicationAdministration>(relaxed = true) {
                        every { id } returns Id("medAdmin2")
                    }
            }

        val bundle =
            mockk<Bundle>(relaxed = true) {
                every { entry } returns listOf(medAdmin1, medAdmin2)
                every { link } returns emptyList()
            }
        coEvery {
            client.get(
                testTenant,
                "/MedicationAdministration",
                any(),
            )
        } returns EHRResponse(mockk { coEvery { body<Bundle>() } returns bundle }, "12345")
        val result =
            service.findMedicationAdministrationsByPatient(
                testTenant,
                "patID",
                LocalDate.now().minusYears(1),
                LocalDate.now(),
            )
        assertEquals(2, result.size)
    }

    @Test
    fun `find by request returns empty list`() {
        assertEquals(
            emptyList<MedicationAdministration>(),
            service.findMedicationAdministrationsByRequest(testTenant, mockk()),
        )
    }
}
