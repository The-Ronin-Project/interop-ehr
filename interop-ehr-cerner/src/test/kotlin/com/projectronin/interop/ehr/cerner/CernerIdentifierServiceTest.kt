package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.toFhirIdentifier
import com.projectronin.interop.tenant.config.model.vendor.Cerner
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CernerIdentifierServiceTest {
    private val service = CernerIdentifierService()
    private val tenant = createTestTenant()

    private val mrnIdentifier = mockk<Identifier> {
        every { system } returns Uri(tenant.vendorAs<Cerner>().patientMRNSystem)
        every { value } returns "mrn".asFHIR()
    }

    private val fhirId = Id("id")
    private val fhirIdentifiers = mockk<FHIRIdentifiers> {
        every { id } returns fhirId
    }
    private val emptyFhirIdentifiers = mockk<FHIRIdentifiers> {
        every { id } returns mockk {
            every { value } returns null
        }
    }

    @Test
    fun `getPractitionerIdentifier throws exception`() {
        assertThrows<NotImplementedError> {
            service.getPractitionerIdentifier(tenant, listOf(mrnIdentifier))
        }
    }

    @Test
    fun `getPatientIdentifier throws exception`() {
        assertThrows<NotImplementedError> {
            service.getPatientIdentifier(tenant, listOf(mrnIdentifier))
        }
    }

    @Test
    fun `getPractitionerProviderIdentifier returns an identifier`() {
        val actualIdentifier = service.getPractitionerProviderIdentifier(tenant, fhirIdentifiers)
        assertEquals(fhirId.toFhirIdentifier(), actualIdentifier)
    }

    @Test
    fun `getPractitionerProviderIdentifier throws exception when id does not have a value`() {
        assertThrows<VendorIdentifierNotFoundException> {
            service.getPractitionerProviderIdentifier(tenant, emptyFhirIdentifiers)
        }
    }

    @Test
    fun `getPractitionerUserIdentifier returns an identifier`() {
        val actualIdentifier = service.getPractitionerUserIdentifier(tenant, fhirIdentifiers)
        assertEquals(fhirId.toFhirIdentifier(), actualIdentifier)
    }

    @Test
    fun `getPractitionerUserIdentifier throws exception when id does not have a value`() {
        assertThrows<VendorIdentifierNotFoundException> {
            service.getPractitionerUserIdentifier(tenant, emptyFhirIdentifiers)
        }
    }

    @Test
    fun `getMRNIdentifier returns an MRN Identifier when one is given`() {
        val actualIdentifier = service.getMRNIdentifier(tenant, listOf(mrnIdentifier))
        assertEquals(mrnIdentifier, actualIdentifier)
    }

    @Test
    fun `getMRNIdentifier returns throws an exception when no mrn is given`() {
        val exception = assertThrows<VendorIdentifierNotFoundException> {
            service.getMRNIdentifier(tenant, listOf())
        }
        assertEquals("No MRN identifier with system '${tenant.vendorAs<Cerner>().patientMRNSystem}' found for Patient", exception.message)
    }

    @Test
    fun `getLocationIdentifier throws exception`() {
        assertThrows<NotImplementedError> {
            service.getLocationIdentifier(tenant, listOf(mrnIdentifier))
        }
    }
}
