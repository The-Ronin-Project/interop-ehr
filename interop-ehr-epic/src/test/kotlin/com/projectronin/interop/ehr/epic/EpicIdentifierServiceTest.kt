package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicIdentifierServiceTest {
    private val epicDepartmentSystem = "urn:oid:1.2.840.114350.1.13.297.3.7.2.686980"
    private val service = EpicIdentifierService()
    private val tenant =
        createTestTenant(
            practitionerProviderSystem = "practitionerProviderSystem",
            practitionerUserSystem = "practitionerUserSystem",
            mrnSystem = "mrnSystem",
            internalSystem = "internalSystem",
            departmentInternalSystem = epicDepartmentSystem,
        )
    private val otherType =
        mockk<CodeableConcept> {
            every { text } returns "Other".asFHIR()
        }
    private val otherValueType =
        mockk<Identifier> {
            every { system } returns null
            every { value } returns "other-value".asFHIR()
            every { type } returns otherType
        }
    private val internalType =
        mockk<CodeableConcept> {
            every { text } returns "Internal".asFHIR()
        }
    private val internalValueType =
        mockk<Identifier> {
            every { system } returns null
            every { value } returns "internal-value".asFHIR()
            every { type } returns internalType
        }
    private val epiclocationIdentifier =
        mockk<Identifier> {
            every { system } returns Uri(epicDepartmentSystem)
            every { value } returns "internal-value".asFHIR()
            every { type } returns internalType
        }
    private val epicOrderIdentifier =
        mockk<Identifier> {
            every { system } returns Uri("orderSystem")
            every { value } returns "orderID".asFHIR()
        }
    private val epicEncounterIdentifier =
        mockk<Identifier> {
            every { system } returns Uri("csnSystem")
            every { value?.value } returns "encCSN"
        }

    @Test
    fun `getPractitionerIdentifier with no matching type`() {
        val identifiers = listOf(otherValueType, otherValueType)
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPractitionerIdentifier(tenant, identifiers)
            }
        assertEquals(
            "No practitioner identifier with system 'practitionerProviderSystem' found",
            exception.message,
        )
    }

    @Test
    fun `getPractitionerIdentifier properly finds external`() {
        val externalIdentifier =
            mockk<Identifier> {
                every { system } returns null
                every { value } returns "external-value".asFHIR()
                every { type } returns
                    mockk {
                        every { text } returns "external".asFHIR()
                    }
            }
        val identifiers = listOf(otherValueType, otherValueType, externalIdentifier)
        val practitionerIdentifier = service.getPractitionerIdentifier(tenant, identifiers)
        assertEquals("practitionerProviderSystem", practitionerIdentifier.system?.value)
        assertEquals("external-value".asFHIR(), practitionerIdentifier.value)
        assertEquals(externalIdentifier.type?.text, practitionerIdentifier.type?.text)
    }

    @Test
    fun `getPractitionerIdentifier properly cascades`() {
        val externalIdentifier =
            mockk<Identifier> {
                every { system } returns null
                every { value } returns "external-value".asFHIR()
                every { type } returns
                    mockk {
                        every { text } returns "external".asFHIR()
                    }
            }
        val identifiers = listOf(internalValueType, otherValueType, externalIdentifier)
        val practitionerIdentifier = service.getPractitionerIdentifier(tenant, identifiers)
        assertEquals("practitionerProviderSystem", practitionerIdentifier.system?.value)
        assertEquals("internal-value".asFHIR(), practitionerIdentifier.value)
        assertEquals(internalType.text, practitionerIdentifier.type?.text)
    }

    @Test
    fun `getPractitionerIdentifier with internal type`() {
        val identifiers = listOf(internalValueType, otherValueType)

        val practitionerIdentifier = service.getPractitionerIdentifier(tenant, identifiers)
        assertEquals("practitionerProviderSystem", practitionerIdentifier.system?.value)
        assertEquals("internal-value".asFHIR(), practitionerIdentifier.value)
        assertEquals(internalType.text, practitionerIdentifier.type?.text)
    }

    @Test
    fun `getPatientIdentifier with null type`() {
        val identifier =
            mockk<Identifier> {
                every { value } returns "other-value".asFHIR()
                every { type } returns null
            }
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPatientIdentifier(tenant, listOf(identifier))
            }
        assertEquals("No matching identifier for the patient with system internalSystem", exception.message)
    }

    @Test
    fun `getPatientIdentifier with type with null text`() {
        val identifier =
            mockk<Identifier> {
                every { value } returns "other-value".asFHIR()
                every { type } returns
                    mockk {
                        every { text } returns null
                    }
            }
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPatientIdentifier(tenant, listOf(identifier))
            }
        assertEquals("No matching identifier for the patient with system internalSystem", exception.message)
    }

    @Test
    fun `getPatientIdentifier with type with text with null value`() {
        val identifier =
            mockk<Identifier> {
                every { value } returns "other-value".asFHIR()
                every { type } returns
                    mockk {
                        every { text } returns
                            mockk {
                                every { value } returns null
                            }
                    }
            }
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPatientIdentifier(tenant, listOf(identifier))
            }
        assertEquals("No matching identifier for the patient with system internalSystem", exception.message)
    }

    @Test
    fun `getPatientIdentifier with no matching type`() {
        val identifiers = listOf(otherValueType, otherValueType)
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPatientIdentifier(tenant, identifiers)
            }
        assertEquals("No matching identifier for the patient with system internalSystem", exception.message)
    }

    @Test
    fun `getPatientIdentifier with internal type`() {
        val identifiers = listOf(internalValueType, otherValueType)

        val patientIdentifier = service.getPatientIdentifier(tenant, identifiers)
        assertEquals("internal-value".asFHIR(), patientIdentifier.value)
        assertEquals(internalType, patientIdentifier.type)
    }

    @Test
    fun `getPatientIdentifier with internal type and system`() {
        val internalIdentifierWithSystem =
            mockk<Identifier> {
                every { value } returns "internal-value".asFHIR()
                every { type } returns internalType
                every { system } returns null
            }

        val identifiers = listOf(internalIdentifierWithSystem, otherValueType)

        val patientIdentifier = service.getPatientIdentifier(tenant, identifiers)
        assertEquals("internalSystem", patientIdentifier.system?.value)
        assertEquals("internal-value".asFHIR(), patientIdentifier.value)
        assertEquals(internalType, patientIdentifier.type)
    }

    private val r4UnknownSystemExternal =
        Identifier(system = Uri("unknown-system"), type = CodeableConcept(text = "External".asFHIR()))
    private val r4PractitionerProviderIdentifierExternal =
        Identifier(system = Uri("practitionerProviderSystem"), type = CodeableConcept(text = "External".asFHIR()))
    private val r4PractitionerProviderIdentifierInternal =
        Identifier(system = Uri("practitionerProviderSystem"), type = CodeableConcept(text = "Internal".asFHIR()))
    private val r4PractitionerUserIdentifierExternal =
        Identifier(system = Uri("practitionerUserSystem"), type = CodeableConcept(text = "External".asFHIR()))
    private val r4PractitionerUserIdentifierInternal =
        Identifier(system = Uri("practitionerUserSystem"), type = CodeableConcept(text = "Internal".asFHIR()))
    private val patientMRNIdentifier = Identifier(system = Uri("mrnSystem"), value = "mrn".asFHIR())

    @Test
    fun `getPractitionerProviderIdentifier with no matching system`() {
        val fhirIdentifiers = FHIRIdentifiers(Id("1234"), listOf(r4UnknownSystemExternal, r4UnknownSystemExternal))
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPractitionerProviderIdentifier(tenant, fhirIdentifiers)
            }
        assertEquals(
            "No practitioner provider identifier with system 'practitionerProviderSystem' found for resource with FHIR id '1234'",
            exception.message,
        )
    }

    @Test
    fun `getPractitionerProviderIdentifier with matching system and null type`() {
        val identifier =
            Identifier(system = Uri("practitionerProviderSystem"), type = null)

        val fhirIdentifiers =
            FHIRIdentifiers(
                Id("1234"),
                listOf(identifier),
            )
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPractitionerProviderIdentifier(tenant, fhirIdentifiers)
            }
        assertEquals(
            "No practitioner provider identifier with system 'practitionerProviderSystem' found for resource with FHIR id '1234'",
            exception.message,
        )
    }

    @Test
    fun `getPractitionerProviderIdentifier with matching system and type with null text`() {
        val identifier =
            Identifier(system = Uri("practitionerProviderSystem"), type = CodeableConcept(text = null))

        val fhirIdentifiers =
            FHIRIdentifiers(
                Id("1234"),
                listOf(identifier),
            )
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPractitionerProviderIdentifier(tenant, fhirIdentifiers)
            }
        assertEquals(
            "No practitioner provider identifier with system 'practitionerProviderSystem' found for resource with FHIR id '1234'",
            exception.message,
        )
    }

    @Test
    fun `getPractitionerProviderIdentifier with matching system and type with text with null value`() {
        val identifier =
            Identifier(system = Uri("practitionerProviderSystem"), type = CodeableConcept(text = FHIRString(null)))

        val fhirIdentifiers =
            FHIRIdentifiers(
                Id("1234"),
                listOf(identifier),
            )
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPractitionerProviderIdentifier(tenant, fhirIdentifiers)
            }
        assertEquals(
            "No practitioner provider identifier with system 'practitionerProviderSystem' found for resource with FHIR id '1234'",
            exception.message,
        )
    }

    @Test
    fun `getPractitionerProviderIdentifier with matching system and no External type`() {
        val fhirIdentifiers =
            FHIRIdentifiers(
                Id("1234"),
                listOf(r4PractitionerProviderIdentifierInternal, r4PractitionerProviderIdentifierInternal),
            )
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPractitionerProviderIdentifier(tenant, fhirIdentifiers)
            }
        assertEquals(
            "No practitioner provider identifier with system 'practitionerProviderSystem' found for resource with FHIR id '1234'",
            exception.message,
        )
    }

    @Test
    fun `getPractitionerProviderIdentifier with matching system and External type`() {
        val fhirIdentifiers =
            FHIRIdentifiers(
                Id("1234"),
                listOf(r4PractitionerProviderIdentifierExternal, r4PractitionerProviderIdentifierInternal),
            )
        val vendorIdentifier = service.getPractitionerProviderIdentifier(tenant, fhirIdentifiers)
        assertEquals(r4PractitionerProviderIdentifierExternal, vendorIdentifier)
    }

    @Test
    fun `getPractitionerUserIdentifier with no matching system`() {
        val fhirIdentifiers = FHIRIdentifiers(Id("1234"), listOf(r4UnknownSystemExternal, r4UnknownSystemExternal))
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPractitionerUserIdentifier(tenant, fhirIdentifiers)
            }
        assertEquals(
            "No practitioner user identifier with system 'practitionerUserSystem' found for resource with FHIR id '1234'",
            "No practitioner user identifier with system 'practitionerUserSystem' found for resource with FHIR id '1234'",
            exception.message,
        )
    }

    @Test
    fun `getPractitionerUserIdentifier with matching system and no External type`() {
        val fhirIdentifiers =
            FHIRIdentifiers(
                Id("1234"),
                listOf(r4PractitionerUserIdentifierInternal, r4PractitionerUserIdentifierInternal),
            )
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getPractitionerUserIdentifier(tenant, fhirIdentifiers)
            }
        assertEquals(
            "No practitioner user identifier with system 'practitionerUserSystem' found for resource with FHIR id '1234'",
            exception.message,
        )
    }

    @Test
    fun `getPractitionerUserIdentifier with matching system and External type`() {
        val fhirIdentifiers =
            FHIRIdentifiers(
                Id("1234"),
                listOf(r4PractitionerUserIdentifierExternal, r4PractitionerUserIdentifierInternal),
            )
        val vendorIdentifier = service.getPractitionerUserIdentifier(tenant, fhirIdentifiers)
        assertEquals(r4PractitionerUserIdentifierExternal, vendorIdentifier)
    }

    @Test
    fun `getMRNIdentifier with no matching system`() {
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getMRNIdentifier(
                    tenant,
                    listOf(Identifier(system = Uri("notTheMRNSystem"), value = "mrn".asFHIR())),
                )
            }
        assertEquals("No MRN identifier with system 'mrnSystem' found for Patient", exception.message)
    }

    @Test
    fun `getMRNIdentifier with matching system`() {
        val mrnIdentifier = service.getMRNIdentifier(tenant, listOf(patientMRNIdentifier))
        assertEquals(patientMRNIdentifier, mrnIdentifier)
    }

    @Test
    fun `getLocationIdentifier with no matching type`() {
        val identifiers = listOf(otherValueType, otherValueType)
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getLocationIdentifier(tenant, identifiers)
            }
        assertEquals(
            "No location identifier with system 'urn:oid:1.2.840.114350.1.13.297.3.7.2.686980' found",
            exception.message,
        )
    }

    @Test
    fun `getLocationIdentifier properly cascades`() {
        val externalIdentifier =
            mockk<Identifier> {
                every { system } returns null
                every { value } returns "external-value".asFHIR()
                every { type } returns
                    mockk {
                        every { text } returns "external".asFHIR()
                    }
            }
        val identifiers = listOf(internalValueType, otherValueType, externalIdentifier)
        val practitionerIdentifier = service.getLocationIdentifier(tenant, identifiers)
        assertEquals(epicDepartmentSystem, practitionerIdentifier.system?.value)
        assertEquals("internal-value".asFHIR(), practitionerIdentifier.value)
        assertEquals(internalType.text, practitionerIdentifier.type?.text)
    }

    @Test
    fun `getLocationIdentifier with internal type`() {
        val identifiers = listOf(epiclocationIdentifier, otherValueType)

        val locationIdentifier = service.getLocationIdentifier(tenant, identifiers)
        assertEquals(epicDepartmentSystem, locationIdentifier.system?.value)
        assertEquals("internal-value".asFHIR(), locationIdentifier.value)
        assertEquals(internalType.text, locationIdentifier.type?.text)
    }

    @Test
    fun `getLocationIdentifier with no matching system`() {
        val identifiers = listOf(Identifier(system = Uri("notTheLocationSystem"), value = "mrn".asFHIR()))
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getLocationIdentifier(tenant, identifiers)
            }
        assertEquals(
            "No location identifier with system 'urn:oid:1.2.840.114350.1.13.297.3.7.2.686980' found",
            exception.message,
        )
    }

    @Test
    fun `getLocationIdentifier with matching system`() {
        val locationIdentifier = service.getLocationIdentifier(tenant, listOf(epiclocationIdentifier))
        assertEquals(epiclocationIdentifier, locationIdentifier)
    }

    @Test
    fun `getOrderIdentifier works`() {
        val orderIdentifier = service.getOrderIdentifier(tenant, listOf(epicOrderIdentifier))
        assertEquals(epicOrderIdentifier, orderIdentifier)
    }

    @Test
    fun `getOrderIdentifier with no matching system`() {
        val identifiers = listOf(Identifier(system = Uri("notTheOrderSystem"), value = "blah".asFHIR()))
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getOrderIdentifier(tenant, identifiers)
            }
        assertEquals(
            "No order identifier found for 'orderSystem'",
            exception.message,
        )
    }

    @Test
    fun `getOrderIdentifier with null matching system`() {
        val identifiers = listOf(Identifier(system = Uri(null), value = null))
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getOrderIdentifier(tenant, identifiers)
            }
        assertEquals(
            "No order identifier found for 'orderSystem'",
            exception.message,
        )
    }

    @Test
    fun `getEncounterIdentifier works`() {
        val encounterIdentifier = service.getEncounterIdentifier(tenant, listOf(epicEncounterIdentifier))
        assertEquals(epicEncounterIdentifier, encounterIdentifier)
    }

    @Test
    fun `getEncounterIdentifier with no matching system`() {
        val identifiers = listOf(Identifier(system = Uri("notTheEncounterIdentifier"), value = "blah".asFHIR()))
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getEncounterIdentifier(tenant, identifiers)
            }
        assertEquals(
            "No encounter identifier found for 'csnSystem'",
            exception.message,
        )
    }

    @Test
    fun `getEncounterIdentifier with null matching system`() {
        val identifiers = listOf(Identifier(system = Uri(null), value = null))
        val exception =
            assertThrows<VendorIdentifierNotFoundException> {
                service.getEncounterIdentifier(tenant, identifiers)
            }
        assertEquals(
            "No encounter identifier found for 'csnSystem'",
            exception.message,
        )
    }
}
