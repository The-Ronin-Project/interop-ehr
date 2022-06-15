package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.model.EpicIDType
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept as R4CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier as R4Identifier

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
    private val otherIdentifier = mockk<EpicIDType> {
        every { system } returns null
        every { value } returns "other-value"
        every { type } returns otherType
        every { raw } returns "{}"
        every { element } returns mockk()
    }
    private val internalType = mockk<CodeableConcept> {
        every { text } returns "Internal"
    }
    private val internalIdentifier = mockk<EpicIDType> {
        every { system } returns null
        every { value } returns "internal-value"
        every { type } returns internalType
        every { raw } returns "{}"
        every { element } returns mockk()
    }
    private val externalType = mockk<CodeableConcept> {
        every { text } returns "External"
    }
    private val externalIdentifier = mockk<EpicIDType> {
        every { system } returns null
        every { value } returns "external-value"
        every { type } returns externalType
        every { raw } returns "{}"
        every { element } returns mockk()
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
    fun `getPractitionerIdentifier with external type`() {
        val identifiers = listOf(externalIdentifier, otherIdentifier)

        val practitionerIdentifier = service.getPractitionerIdentifier(tenant, identifiers)
        assertEquals("practitionerProviderSystem", practitionerIdentifier.system)
        assertEquals("external-value", practitionerIdentifier.value)
        assertEquals(externalType, practitionerIdentifier.type)
    }

    @Test
    fun `getPractitionerIdentifier with internal type`() {
        val identifiers = listOf(internalIdentifier, otherIdentifier)

        val practitionerIdentifier = service.getPractitionerIdentifier(tenant, identifiers)
        assertEquals("practitionerProviderSystem", practitionerIdentifier.system)
        assertEquals("internal-value", practitionerIdentifier.value)
        assertEquals(internalType, practitionerIdentifier.type)
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
    fun `getPatientIdentifier with external type`() {
        val identifiers = listOf(externalIdentifier, otherIdentifier)

        val patientIdentifier = service.getPatientIdentifier(tenant, identifiers)
        assertEquals("external-value", patientIdentifier.value)
        assertEquals(externalType, patientIdentifier.type)
    }

    @Test
    fun `getPatientIdentifier with external type and system`() {
        val externalIdentifierWithSystem = mockk<EpicIDType> {
            every { value } returns "external-value"
            every { type } returns externalType
            every { raw } returns "{}"
            every { element } returns mockk()
        }

        val identifiers = listOf(externalIdentifierWithSystem, otherIdentifier)

        val patientIdentifier = service.getPatientIdentifier(tenant, identifiers)
        assertEquals("internalSystem", patientIdentifier.system)
        assertEquals("external-value", patientIdentifier.value)
        assertEquals(externalType, patientIdentifier.type)
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
        val internalIdentifierWithSystem = mockk<EpicIDType> {
            every { value } returns "internal-value"
            every { type } returns internalType
            every { raw } returns "{}"
            every { element } returns mockk()
        }

        val identifiers = listOf(internalIdentifierWithSystem, otherIdentifier)

        val patientIdentifier = service.getPatientIdentifier(tenant, identifiers)
        assertEquals("internalSystem", patientIdentifier.system)
        assertEquals("internal-value", patientIdentifier.value)
        assertEquals(internalType, patientIdentifier.type)
    }

    @Test
    fun `getPatientIdentifier with a non-EpicIDType`() {
        val nonEpicIDTypeIdentifier = mockk<Identifier> {
            every { value } returns "internal-value"
            every { type } returns internalType
            every { raw } returns "{}"
            every { element } returns mockk()
        }

        val identifiers = listOf(nonEpicIDTypeIdentifier)
        assertThrows<IllegalArgumentException> { service.getPatientIdentifier(tenant, identifiers) }
    }

    private val r4UnknownSystemExternal =
        R4Identifier(system = Uri("unknown-system"), type = R4CodeableConcept(text = "External"))
    private val r4PractitionerProviderIdentifierExternal =
        R4Identifier(system = Uri("practitionerProviderSystem"), type = R4CodeableConcept(text = "External"))
    private val r4PractitionerProviderIdentifierInternal =
        R4Identifier(system = Uri("practitionerProviderSystem"), type = R4CodeableConcept(text = "Internal"))
    private val r4PractitionerUserIdentifierExternal =
        R4Identifier(system = Uri("practitionerUserSystem"), type = R4CodeableConcept(text = "External"))
    private val r4PractitionerUserIdentifierInternal =
        R4Identifier(system = Uri("practitionerUserSystem"), type = R4CodeableConcept(text = "Internal"))
    private val patientMRNIdentifier = R4Identifier(system = Uri("mrnSystem"), value = "mrn")

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
        assertFalse(vendorIdentifier.isFhirId)
        assertEquals(r4PractitionerProviderIdentifierExternal, vendorIdentifier.identifier)
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
        assertFalse(vendorIdentifier.isFhirId)
        assertEquals(r4PractitionerUserIdentifierExternal, vendorIdentifier.identifier)
    }

    @Test
    fun `getMRNIdentifier with no matching system`() {
        val exception = assertThrows<VendorIdentifierNotFoundException> {
            service.getMRNIdentifier(tenant, listOf(R4Identifier(system = Uri("notTheMRNSystem"), value = "mrn")))
        }
        assertEquals("No MRN identifier with system 'mrnSystem' found for Patient", exception.message)
    }

    @Test
    fun `getMRNIdentifier with matching system`() {
        val mrnIdentifier = service.getMRNIdentifier(tenant, listOf(patientMRNIdentifier))
        assertEquals(patientMRNIdentifier, mrnIdentifier)
    }

    @Test
    fun `can serialize StandardizedIdentifier`() {
        val sourceIdentifier = IDType("id", "value")
        val standardizedIdentifier =
            EpicIdentifierService.StandardizedIdentifier("system", EpicIDType(sourceIdentifier))
        val json =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(standardizedIdentifier)
        val expectedJson = """
            {
              "system" : "system",
              "element" : {
                "ID" : "id",
                "Type" : "value"
              }
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize StandardizedIdentifier`() {
        val json = """
            {
              "system" : "system",
              "element" : {
                "ID" : "id",
                "Type" : "typeValue"
              }
            }
        """.trimIndent()

        val deserializedIdentifier =
            JacksonManager.objectMapper.readValue(json, EpicIdentifierService.StandardizedIdentifier::class.java)
        assertEquals("typeValue", deserializedIdentifier.type?.text)
        assertEquals("system", deserializedIdentifier.system)
        assertEquals("id", deserializedIdentifier.value)
    }

    @Test
    fun `can deserialize StandardizedIdentifier with null`() {
        val json = """
            {
              "system" : null,
              "element" : {
                "ID" : "id",
                "Type" : "typeValue"
              }
            }
        """.trimIndent()

        val deserializedIdentifier =
            JacksonManager.objectMapper.readValue(json, EpicIdentifierService.StandardizedIdentifier::class.java)
        assertEquals("typeValue", deserializedIdentifier.type?.text)
        assertNull(deserializedIdentifier.system)
        assertEquals("id", deserializedIdentifier.value)
    }

    @Test
    fun `can serialize and deserialize StandardizedIdentifier`() {
        val sourceIdentifier = IDType("id", "value")
        val standardizedIdentifier =
            EpicIdentifierService.StandardizedIdentifier("system", EpicIDType(sourceIdentifier))
        val json =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(standardizedIdentifier)
        val deserializedIdentifier =
            JacksonManager.objectMapper.readValue(json, EpicIdentifierService.StandardizedIdentifier::class.java)
        assertEquals(standardizedIdentifier, deserializedIdentifier)
    }
}
