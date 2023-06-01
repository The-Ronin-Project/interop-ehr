package com.interop.ehr.hl7

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.tenant.config.data.TenantMDMConfigDAO
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MDMConfigServiceTest {
    private val mdmConfigDAO = mockk<TenantMDMConfigDAO>()
    private val mdmConfigService = MDMConfigService(mdmConfigDAO)
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "blank"
        every { vendor } returns mockk<Epic> {
            every { type } returns VendorType.EPIC
            every { patientMRNSystem } returns "mrn"
        }
    }

    @Test
    fun `getIdentifiersToSend works`() {
        val mockIdent = mockk<Identifier>() {
            every { system?.value } returns "http://projectronin.com/id/mrn"
        }
        val mockIdent2 = mockk<Identifier>() {
            every { system?.value } returns "mrn"
        }
        val mockIdent3 = mockk<Identifier>() {
            every { system?.value } returns "notMrn"
        }
        val result = mdmConfigService.getIdentifiersToSend(tenant, listOf(mockIdent, mockIdent2, mockIdent3))
        assertEquals(mockIdent, result.first())
    }

    @Test
    fun `getIdentifiersToSend works with no ronin`() {
        val mockIdent = mockk<Identifier>() {
            every { system?.value } returns "mrn"
        }
        val mockIdent2 = mockk<Identifier>() {
            every { system?.value } returns "notMrn"
        }
        val result = mdmConfigService.getIdentifiersToSend(tenant, listOf(mockIdent, mockIdent2))
        assertEquals(mockIdent, result.first())
    }

    @Test
    fun `getIdentifiersToSend throws error`() {
        val mockIdent = mockk<Identifier>() {
            every { system?.value } returns "notMrn"
        }
        assertThrows<VendorIdentifierNotFoundException> {
            mdmConfigService.getIdentifiersToSend(
                tenant,
                listOf(mockIdent)
            )
        }
    }

    @Test
    fun `getPractitionerIdentifierToSend works`() {
        val mockIdent = mockk<Identifier>() {
            every { system?.value } returns "system"
        }
        val mockIdent2 = mockk<Identifier>() {
            every { system?.value } returns "notSystem"
        }
        every { mdmConfigDAO.getByTenantMnemonic(any()) } returns mockk {
            every { providerIdentifierSystem } returns "system"
        }
        val result = mdmConfigService.getPractitionerIdentifierToSend(tenant, listOf(mockIdent, mockIdent2))
        assertEquals(mockIdent, result)
    }

    @Test
    fun `getDocumentTypeID works`() {
        every { mdmConfigDAO.getByTenantMnemonic(any()) } returns mockk {
            every { mdmDocumentTypeID } returns "123"
        }
        val result = mdmConfigService.getDocumentTypeID(tenant)
        assertEquals("123", result)
    }
}
