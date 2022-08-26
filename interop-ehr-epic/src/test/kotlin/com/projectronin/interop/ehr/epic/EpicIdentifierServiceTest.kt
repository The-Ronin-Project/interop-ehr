package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicIdentifierServiceTest {
    private val service = EpicIdentifierService()
    private val tenant = createTestTenant(
        practitionerProviderSystem = "practitionerProviderSystem",
        practitionerUserSystem = "practitionerUserSystem",
        mrnSystem = "mrnSystem",
        internalSystem = "internalSystem"
    )

    private val otherType = mockk<CodeableConcept> {
        every { text } returns "Other"
    }
    private val otherIdentifier = mockk<Identifier> {
        every { system } returns null
        every { value } returns "other-value"
        every { type } returns otherType
    }
    private val internalType = mockk<CodeableConcept> {
        every { text } returns "Internal"
    }
    private val internalIdentifier = mockk<Identifier> {
        every { system } returns null
        every { value } returns "internal-value"
        every { type } returns internalType
    }

    @Test
    fun `getPractitionerIdentifier with no matching type`() {
        val identifiers = listOf(otherIdentifier, otherIdentifier)
        val exception = assertThrows<VendorIdentifierNotFoundException> {
            service.getPractitionerIdentifier(tenant, identifiers)
        }
        assertEquals("No identifier found for practitioner", exception.message)
    }

    @Test
    fun `getPractitionerIdentifier with internal type`() {
        val identifiers = listOf(internalIdentifier, otherIdentifier)

        val practitionerIdentifier = service.getPractitionerIdentifier(tenant, identifiers)
        assertEquals("practitionerProviderSystem", practitionerIdentifier.system?.value)
        assertEquals("internal-value", practitionerIdentifier.value)
        assertEquals(internalType.text, practitionerIdentifier.type?.text)
    }

    @Test
    fun `getPatientIdentifier with no matching type`() {
        val identifiers = listOf(otherIdentifier, otherIdentifier)
        val exception = assertThrows<VendorIdentifierNotFoundException> {
            service.getPatientIdentifier(tenant, identifiers)
        }
        assertEquals("No matching identifier for the patient with system internalSystem", exception.message)
    }

    @Test
    fun `getPatientIdentifier with internal type`() {
        val identifiers = listOf(internalIdentifier, otherIdentifier)

        val patientIdentifier = service.getPatientIdentifier(tenant, identifiers)
        assertEquals("internal-value", patientIdentifier.value)
        assertEquals(internalType, patientIdentifier.type)
    }

    @Test
    fun `getPatientIdentifier with internal type and system`() {
        val internalIdentifierWithSystem = mockk<Identifier> {
            every { value } returns "internal-value"
            every { type } returns internalType
            every { system } returns null
        }

        val identifiers = listOf(internalIdentifierWithSystem, otherIdentifier)

        val patientIdentifier = service.getPatientIdentifier(tenant, identifiers)
        assertEquals("internalSystem", patientIdentifier.system?.value)
        assertEquals("internal-value", patientIdentifier.value)
        assertEquals(internalType, patientIdentifier.type)
    }

    private val r4UnknownSystemExternal =
        Identifier(system = Uri("unknown-system"), type = CodeableConcept(text = "External"))
    private val r4PractitionerProviderIdentifierExternal =
        Identifier(system = Uri("practitionerProviderSystem"), type = CodeableConcept(text = "External"))
    private val r4PractitionerProviderIdentifierInternal =
        Identifier(system = Uri("practitionerProviderSystem"), type = CodeableConcept(text = "Internal"))
    private val r4PractitionerUserIdentifierExternal =
        Identifier(system = Uri("practitionerUserSystem"), type = CodeableConcept(text = "External"))
    private val r4PractitionerUserIdentifierInternal =
        Identifier(system = Uri("practitionerUserSystem"), type = CodeableConcept(text = "Internal"))
    private val patientMRNIdentifier = Identifier(system = Uri("mrnSystem"), value = "mrn")

    @Test
    fun `getPractitionerProviderIdentifier with no matching system`() {
        val fhirIdentifiers = FHIRIdentifiers(Id("1234"), listOf(r4UnknownSystemExternal, r4UnknownSystemExternal))
        val exception = assertThrows<VendorIdentifierNotFoundException> {
            service.getPractitionerProviderIdentifier(tenant, fhirIdentifiers)
        }
        assertEquals(
            "No practitioner provider identifier with system 'practitionerProviderSystem' found for resource with FHIR id '1234'",
            exception.message
        )
    }

    @Test
    fun `getPractitionerProviderIdentifier with matching system and no External type`() {
        val fhirIdentifiers = FHIRIdentifiers(
            Id("1234"),
            listOf(r4PractitionerProviderIdentifierInternal, r4PractitionerProviderIdentifierInternal)
        )
        val exception = assertThrows<VendorIdentifierNotFoundException> {
            service.getPractitionerProviderIdentifier(tenant, fhirIdentifiers)
        }
        assertEquals(
            "No practitioner provider identifier with system 'practitionerProviderSystem' found for resource with FHIR id '1234'",
            exception.message
        )
    }

    @Test
    fun `getPractitionerProviderIdentifier with matching system and External type`() {
        val fhirIdentifiers = FHIRIdentifiers(
            Id("1234"),
            listOf(r4PractitionerProviderIdentifierExternal, r4PractitionerProviderIdentifierInternal)
        )
        val vendorIdentifier = service.getPractitionerProviderIdentifier(tenant, fhirIdentifiers)
        assertEquals(r4PractitionerProviderIdentifierExternal, vendorIdentifier)
    }

    @Test
    fun `getPractitionerUserIdentifier with no matching system`() {
        val fhirIdentifiers = FHIRIdentifiers(Id("1234"), listOf(r4UnknownSystemExternal, r4UnknownSystemExternal))
        val exception = assertThrows<VendorIdentifierNotFoundException> {
            service.getPractitionerUserIdentifier(tenant, fhirIdentifiers)
        }
        assertEquals(
            "No practitioner user identifier with system 'practitionerUserSystem' found for resource with FHIR id '1234'",
            exception.message
        )
    }

    @Test
    fun `getPractitionerUserIdentifier with matching system and no External type`() {
        val fhirIdentifiers = FHIRIdentifiers(
            Id("1234"),
            listOf(r4PractitionerUserIdentifierInternal, r4PractitionerUserIdentifierInternal)
        )
        val exception = assertThrows<VendorIdentifierNotFoundException> {
            service.getPractitionerUserIdentifier(tenant, fhirIdentifiers)
        }
        assertEquals(
            "No practitioner user identifier with system 'practitionerUserSystem' found for resource with FHIR id '1234'",
            exception.message
        )
    }

    @Test
    fun `getPractitionerUserIdentifier with matching system and External type`() {
        val fhirIdentifiers = FHIRIdentifiers(
            Id("1234"),
            listOf(r4PractitionerUserIdentifierExternal, r4PractitionerUserIdentifierInternal)
        )
        val vendorIdentifier = service.getPractitionerUserIdentifier(tenant, fhirIdentifiers)
        assertEquals(r4PractitionerUserIdentifierExternal, vendorIdentifier)
    }

    @Test
    fun `getMRNIdentifier with no matching system`() {
        val exception = assertThrows<VendorIdentifierNotFoundException> {
            service.getMRNIdentifier(tenant, listOf(Identifier(system = Uri("notTheMRNSystem"), value = "mrn")))
        }
        assertEquals("No MRN identifier with system 'mrnSystem' found for Patient", exception.message)
    }

    @Test
    fun `getMRNIdentifier with matching system`() {
        val mrnIdentifier = service.getMRNIdentifier(tenant, listOf(patientMRNIdentifier))
        assertEquals(patientMRNIdentifier, mrnIdentifier)
    }
}
