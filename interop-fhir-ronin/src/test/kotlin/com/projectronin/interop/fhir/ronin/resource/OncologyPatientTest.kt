package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Communication
import com.projectronin.interop.fhir.r4.datatype.Contact
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.PatientLink
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.LinkType
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OncologyPatientTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val identifierList = listOf(
        Identifier(
            type = CodeableConcept(
                text = "MRN"
            ),
            system = Uri("mrnSystem"),
            value = "An MRN"
        )
    )

    private val identifierService = mockk<IdentifierService> {
        every { getMRNIdentifier(tenant, identifierList) } returns identifierList[0]
        every { getMRNIdentifier(tenant, emptyList()) } throws VendorIdentifierNotFoundException()
    }
    private val oncologyPatient: OncologyPatient = OncologyPatient.create(identifierService)

    @Test
    fun `validate fails if no tenant identifier provided`() {
        val patient =
            Patient(
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.MRN.uri,
                        type = CodeableConcepts.MRN,
                        value = "MRN"
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.FHIR_STU3_ID,
                        value = "fhirId"
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                oncologyPatient.validate(patient)
            }
        assertEquals("Tenant identifier is required", exception.message)
    }

    @Test
    fun `validate fails if tenant does not have tenant codeable concept`() {
        val patient =
            Patient(
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.MRN,
                        value = "tenantId"
                    ),
                    Identifier(
                        system = CodeSystem.MRN.uri,
                        type = CodeableConcepts.MRN,
                        value = "MRN"
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.FHIR_STU3_ID,
                        value = "fhirId"
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                oncologyPatient.validate(patient)
            }
        assertEquals("Tenant identifier provided without proper CodeableConcept defined", exception.message)
    }

    @Test
    fun `validate fails if tenant does not have value`() {
        val patient =
            Patient(
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.RONIN_TENANT
                    ),
                    Identifier(
                        system = CodeSystem.MRN.uri,
                        type = CodeableConcepts.MRN,
                        value = "MRN"
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.FHIR_STU3_ID,
                        value = "fhirId"
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                oncologyPatient.validate(patient)
            }
        assertEquals("tenant identifier value is required", exception.message)
    }

    @Test
    fun `validate fails if no mrn identifier provided`() {
        val patient =
            Patient(
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "tenantId"
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.FHIR_STU3_ID,
                        value = "fhirId"
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                oncologyPatient.validate(patient)
            }
        assertEquals("mrn identifier is required", exception.message)
    }

    @Test
    fun `validate fails if mrn has a bad codeable concept`() {
        val patient =
            Patient(
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "tenantId"
                    ),
                    Identifier(
                        system = CodeSystem.MRN.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "MRN"
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.FHIR_STU3_ID,
                        value = "fhirId"
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                oncologyPatient.validate(patient)
            }
        assertEquals("mrn identifier type defined without proper CodeableConcept", exception.message)
    }

    @Test
    fun `validate fails if mrn does not have value`() {
        val patient =
            Patient(
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "tenantId"
                    ),
                    Identifier(
                        system = CodeSystem.MRN.uri,
                        type = CodeableConcepts.MRN
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.FHIR_STU3_ID,
                        value = "fhirId"
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                oncologyPatient.validate(patient)
            }
        assertEquals("mrn value is required", exception.message)
    }

    @Test
    fun `validate fails if no fhir_stu3_id identifier provided`() {
        val patient =
            Patient(
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "tenantId"
                    ),
                    Identifier(
                        system = CodeSystem.MRN.uri,
                        type = CodeableConcepts.MRN,
                        value = "MRN"
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                oncologyPatient.validate(patient)
            }
        assertEquals("fhir_stu3_id identifier is required", exception.message)
    }

    @Test
    fun `validate fails if fhir_stu3_id has a bad codeable concept`() {
        val patient =
            Patient(
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "tenantId"
                    ),
                    Identifier(
                        system = CodeSystem.MRN.uri,
                        type = CodeableConcepts.MRN,
                        value = "MRN"
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "fhirId"
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                oncologyPatient.validate(patient)
            }
        assertEquals(
            "fhir_stu3_id identifier type defined without proper CodeableConcept",
            exception.message
        )
    }

    @Test
    fun `validate fails if fhir_stu3_id does not have value`() {
        val patient =
            Patient(
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "tenantId"
                    ),
                    Identifier(
                        system = CodeSystem.MRN.uri,
                        type = CodeableConcepts.MRN,
                        value = "MRN"
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.FHIR_STU3_ID
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                oncologyPatient.validate(patient)
            }
        assertEquals("fhir_stu3_id value is required", exception.message)
    }

    @Test
    fun `validate fails if no name provided`() {
        val patient =
            Patient(
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "tenantId"
                    ),
                    Identifier(
                        system = CodeSystem.MRN.uri,
                        type = CodeableConcepts.MRN,
                        value = "MRN"
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.FHIR_STU3_ID,
                        value = "fhirId"
                    )
                ),
                name = listOf(),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        val exception =
            assertThrows<IllegalArgumentException> {
                oncologyPatient.validate(patient)
            }
        assertEquals("At least one name must be provided", exception.message)
    }

    @Test
    fun `validate passes for valid patient`() {
        val patient = Patient(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                ),
                Identifier(
                    system = CodeSystem.MRN.uri,
                    type = CodeableConcepts.MRN,
                    value = "MRN"
                ),
                Identifier(
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    type = CodeableConcepts.FHIR_STU3_ID,
                    value = "fhirId"
                )
            ),
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    value = "8675309",
                    use = ContactPointUse.MOBILE
                )
            ),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M")
        )
        oncologyPatient.validate(patient)
    }

    @Test
    fun `transforms patient with all attributes`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-patient"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = identifierList,
            active = true,
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    use = ContactPointUse.MOBILE,
                    value = "8675309"
                )
            ),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            deceased = DynamicValue(DynamicValueType.BOOLEAN, false),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M"),
            multipleBirth = DynamicValue(DynamicValueType.INTEGER, 2),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            contact = listOf(Contact(name = HumanName(text = "Jane Doe"))),
            communication = listOf(Communication(language = CodeableConcept(text = "English"))),
            generalPractitioner = listOf(Reference(display = "GP")),
            managingOrganization = Reference(display = "organization"),
            link = listOf(PatientLink(other = Reference(display = "other patient"), type = LinkType.REPLACES))
        )

        val oncologyPatient = oncologyPatient.transform(patient, tenant)

        oncologyPatient!! // Force it to be treated as non-null
        assertEquals("Patient", oncologyPatient.resourceType)
        assertEquals(Id("test-12345"), oncologyPatient.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-patient"))),
            oncologyPatient.meta
        )
        assertEquals(Uri("implicit-rules"), oncologyPatient.implicitRules)
        assertEquals(Code("en-US"), oncologyPatient.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), oncologyPatient.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            oncologyPatient.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyPatient.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyPatient.modifierExtension
        )
        assertEquals(
            listOf(
                identifierList.first(),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test"),
                Identifier(
                    type = CodeableConcepts.FHIR_STU3_ID,
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    value = "12345"
                ),
                Identifier(type = CodeableConcepts.MRN, system = CodeSystem.MRN.uri, value = "An MRN"),
            ),
            oncologyPatient.identifier
        )
        assertEquals(true, oncologyPatient.active)
        assertEquals(listOf(HumanName(family = "Doe")), oncologyPatient.name)
        assertEquals(
            listOf(ContactPoint(value = "8675309", system = ContactPointSystem.PHONE, use = ContactPointUse.MOBILE)),
            oncologyPatient.telecom
        )
        assertEquals(AdministrativeGender.FEMALE, oncologyPatient.gender)
        assertEquals(Date("1975-07-05"), oncologyPatient.birthDate)
        assertEquals(DynamicValue(type = DynamicValueType.BOOLEAN, value = false), oncologyPatient.deceased)
        assertEquals(listOf(Address(country = "USA")), oncologyPatient.address)
        assertEquals(CodeableConcept(text = "M"), oncologyPatient.maritalStatus)
        assertEquals(DynamicValue(type = DynamicValueType.INTEGER, value = 2), oncologyPatient.multipleBirth)
        assertEquals(
            listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            oncologyPatient.photo
        )
        assertEquals(listOf(Communication(language = CodeableConcept(text = "English"))), oncologyPatient.communication)
        assertEquals(listOf(Reference(display = "GP")), oncologyPatient.generalPractitioner)
        assertEquals(Reference(display = "organization"), oncologyPatient.managingOrganization)
        assertEquals(
            listOf(PatientLink(other = Reference(display = "other patient"), type = LinkType.REPLACES)),
            oncologyPatient.link
        )
    }

    @Test
    fun `transforms patient with only required attributes`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = identifierList,
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE,
        )

        val oncologyPatient = oncologyPatient.transform(patient, tenant)

        val defaultCoding = Coding(
            system = Uri("http://terminology.hl7.org/CodeSystem/v3-NullFlavor"),
            code = Code("NI"),
            display = "NoInformation"
        )
        oncologyPatient!! // Force it to be treated as non-null
        assertEquals("Patient", oncologyPatient.resourceType)
        assertEquals(Id("test-12345"), oncologyPatient.id)
        assertNull(oncologyPatient.meta)
        assertNull(oncologyPatient.implicitRules)
        assertNull(oncologyPatient.language)
        assertNull(oncologyPatient.text)
        assertEquals(listOf<ContainedResource>(), oncologyPatient.contained)
        assertEquals(listOf<Extension>(), oncologyPatient.extension)
        assertEquals(listOf<Extension>(), oncologyPatient.modifierExtension)
        assertEquals(
            listOf(
                identifierList.first(),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test"),
                Identifier(
                    type = CodeableConcepts.FHIR_STU3_ID,
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    value = "12345"
                ),
                Identifier(type = CodeableConcepts.MRN, system = CodeSystem.MRN.uri, value = "An MRN"),
            ),
            oncologyPatient.identifier
        )
        assertNull(oncologyPatient.active)
        assertEquals(listOf(HumanName(family = "Doe")), oncologyPatient.name)
        assertEquals(emptyList<ContactPoint>(), oncologyPatient.telecom)
        assertEquals(AdministrativeGender.FEMALE, oncologyPatient.gender)
        assertNull(oncologyPatient.birthDate)
        assertNull(oncologyPatient.deceased)
        assertEquals(emptyList<Address>(), oncologyPatient.address)
        assertEquals(listOf(defaultCoding), oncologyPatient.maritalStatus?.coding)
        assertNull(oncologyPatient.multipleBirth)
        assertEquals(listOf<Attachment>(), oncologyPatient.photo)
        assertEquals(listOf<Communication>(), oncologyPatient.communication)
        assertEquals(listOf<Reference>(), oncologyPatient.generalPractitioner)
        assertNull(oncologyPatient.managingOrganization)
        assertEquals(listOf<PatientLink>(), oncologyPatient.link)
    }

    @Test
    fun `transform fails for patient with missing id`() {
        val patient = Patient(
            identifier = identifierList,
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    use = ContactPointUse.MOBILE,
                    value = "8675309"
                )
            ),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M")
        )

        val oncologyPatient = oncologyPatient.transform(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `transform fails for missing gender`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = identifierList,
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    use = ContactPointUse.MOBILE,
                    value = "8675309"
                )
            ),
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M")
        )

        val oncologyPatient = oncologyPatient.transform(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `transform fails for missing MRN`() {
        val patient = Patient(
            id = Id("12345"),
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    use = ContactPointUse.MOBILE,
                    value = "8675309"
                )
            ),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M")
        )

        val oncologyPatient = oncologyPatient.transform(patient, tenant)
        assertNull(oncologyPatient)
    }
}
