package com.projectronin.interop.ehr.util

import com.projectronin.ehr.dataauthority.models.Identifier
import com.projectronin.ehr.dataauthority.models.IdentifierSearchResponse
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.fhir.r4.CodeSystem
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class EHRDAUtilTest {

    @Test
    fun `getFHIRId - multiple resources`() {
        val mockResponse = mockk<IdentifierSearchResponse> {
            every { searchedIdentifier } returns mockk()
            every { foundResources.size } returns 2
        }
        assertThrows<VendorIdentifierNotFoundException> {
            mockResponse.getFHIRId()
        }
    }

    @Test
    fun `getFHIRId - weird coverage`() {
        val mockResponse = mockk<IdentifierSearchResponse> {
            every { searchedIdentifier } returns mockk()
            every { foundResources } returns listOf()
        }
        assertThrows<NoSuchElementException> {
            mockResponse.getFHIRId()
        }
    }

    @Test
    fun `getFHIRId - no FHIR ID`() {
        val mockResponse = mockk<IdentifierSearchResponse> {
            every { searchedIdentifier } returns mockk()
            every { foundResources } returns listOf(
                mockk {
                    every { identifiers } returns listOf()
                }
            )
        }
        assertThrows<VendorIdentifierNotFoundException> {
            mockResponse.getFHIRId()
        }
    }

    @Test
    fun `getFHIRId - works`() {
        val mockResponse = mockk<IdentifierSearchResponse> {
            every { searchedIdentifier } returns mockk()
            every { foundResources } returns listOf(
                mockk {
                    every { identifiers } returns listOf(
                        mockk {
                            every { value } returns "FHIRID"
                            every { system } returns CodeSystem.RONIN_FHIR_ID.uri.value!!
                        }
                    )
                }
            )
        }
        assertEquals("FHIRID", mockResponse.getFHIRId())
    }

    @Test
    fun `associateFHIRId - works`() {
        val id = Identifier("SYSTEM", "ID")
        val mockResponse = listOf<IdentifierSearchResponse>(
            mockk {
                every { searchedIdentifier } returns id
                every { foundResources } returns listOf(
                    mockk {
                        every { identifiers } returns listOf(
                            mockk {
                                every { value } returns "FHIRID"
                                every { system } returns CodeSystem.RONIN_FHIR_ID.uri.value!!
                            }
                        )
                    }
                )
            }
        )
        val map = mockResponse.associateFHIRId()
        assertEquals("FHIRID", map[id])
    }

    @Test
    fun `associateFHIRId - empty`() {
        val mockResponse = listOf<IdentifierSearchResponse>()
        val map = mockResponse.associateFHIRId()
        assertEquals(map.size, 0)
    }
}
