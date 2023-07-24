package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Medication
import io.ktor.client.statement.HttpResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CernerMedicationServiceTest {
    private val client = mockk<CernerClient>()
    private val service = spyk(CernerMedicationService(client, 5))
    private val tenant = createTestTenant()

    private val med1Id = "123"
    private val med1 = mockk<Medication> {
        every { id } returns mockk {
            every { value } returns med1Id
        }
        every { resourceType } returns "Medication"
    }
    private val med2Id = "456"
    private val med2 = mockk<Medication> {
        every { id } returns mockk {
            every { value } returns med2Id
        }
        every { resourceType } returns "Medication"
    }

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

    @Test
    fun `getByIDs works`() {
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
        assertEquals(
            service.getByIDs(mockk(), listOf("123")).values.toList(),
            listOf(medication)
        )
    }

    @Test
    fun `getByIDs - works with multiple medications`() {
        val parameters = mapOf("_id" to listOf(med1Id, med2Id))

        val bundle = mockk<Bundle> {
            every { entry } returns listOf(
                mockk { every { resource } returns med1 },
                mockk { every { resource } returns med2 }
            )
        }

        every { service.getBundleWithPaging(tenant, parameters) } returns bundle

        val results = service.getByIDs(
            tenant,
            listOf(med1Id, med2Id)
        ).values.toList()

        assertEquals(2, results.size)
        assertEquals(med1, results[0])
        assertEquals(med2, results[1])
    }

    @Test
    fun `getByIDs - works with no medications`() {
        val parameters = mapOf("_id" to "")

        val bundle = mockk<Bundle> {
            every { entry } returns listOf()
        }

        every { service.getBundleWithPaging(tenant, parameters) } returns bundle

        val results = service.getByIDs(tenant, listOf()).values.toList()

        assertEquals(0, results.size)
    }

    @Test
    fun `getByIDs - works with multiple batches`() {
        val med3Id = "789"
        val med3 = mockk<Medication> {
            every { id } returns mockk {
                every { value } returns med3Id
            }
            every { resourceType } returns "Medication"
        }

        val parameters1 = mapOf("_id" to listOf(med1Id, med2Id))
        val parameters2 = mapOf("_id" to listOf(med3Id))

        val bundle1 = mockk<Bundle> {
            every { entry } returns listOf(
                mockk { every { resource } returns med1 },
                mockk { every { resource } returns med2 }
            )
        }
        val bundle2 = mockk<Bundle> {
            every { entry } returns listOf(
                mockk { every { resource } returns med3 }
            )
        }

        val medicationService = spyk(CernerMedicationService(client, 2))
        every { medicationService.getBundleWithPaging(tenant, parameters1) } returns bundle1
        every { medicationService.getBundleWithPaging(tenant, parameters2) } returns bundle2

        val results = medicationService.getByIDs(
            tenant,
            listOf(med1Id, med2Id, med3Id)
        ).values.toList()

        assertEquals(3, results.size)
        assertEquals(med1, results[0])
        assertEquals(med2, results[1])
        assertEquals(med3, results[2])
    }
}
