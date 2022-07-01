package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.ehr.model.enums.DataSource
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
import com.projectronin.interop.fhir.r4.resource.Patient as R4Patient

class R4PatientTransformerTest {
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
    private val mockIdentifierService = mockk<IdentifierService> {
        every { getMRNIdentifier(tenant, identifierList) } returns identifierList[0]
        every { getMRNIdentifier(tenant, emptyList()) } throws VendorIdentifierNotFoundException()
    }
    private val transformer = R4PatientTransformer(mockIdentifierService)

    @Test
    fun `transforms patient with all attributes`() {
        val r4Patient = R4Patient(
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Patient
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)

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
        assertEquals(listOf(PatientLink(other = Reference(display = "other patient"), type = LinkType.REPLACES)), oncologyPatient.link)
    }

    @Test
    fun `transforms patient with only required attributes`() {
        val r4Patient = R4Patient(
            id = Id("12345"),
            identifier = identifierList,
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE,
        )
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Patient
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
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
        assertEquals(listOf(defaultCoding), oncologyPatient.maritalStatus.coding)
        assertNull(oncologyPatient.multipleBirth)
        assertEquals(listOf<Attachment>(), oncologyPatient.photo)
        assertEquals(listOf<Communication>(), oncologyPatient.communication)
        assertEquals(listOf<Reference>(), oncologyPatient.generalPractitioner)
        assertNull(oncologyPatient.managingOrganization)
        assertEquals(listOf<PatientLink>(), oncologyPatient.link)
    }

    @Test
    fun `non R4 patient`() {
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformPatient(patient, tenant)
        }

        assertEquals("Patient is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `fails for patient with missing id`() {
        val r4Patient = R4Patient(
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Patient
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for missing gender`() {
        val r4Patient = R4Patient(
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Patient
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `non R4 bundle`() {
        val bundle = mockk<Bundle<Patient>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformPatients(bundle, tenant)
        }

        assertEquals("Bundle is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `bundle transformation returns empty when no valid transformations`() {
        val invalidPatient = R4Patient(
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
        val patient1 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidPatient
        }
        val patient2 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidPatient
        }

        val bundle = mockk<Bundle<Patient>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(patient1, patient2)
        }

        val oncologyPatients = transformer.transformPatients(bundle, tenant)
        assertEquals(0, oncologyPatients.size)
    }

    @Test
    fun `bundle transformation returns only valid transformations`() {
        val invalidPatient = R4Patient(
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

        val patient1 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidPatient
        }

        val r4Patient = R4Patient(
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
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M")
        )

        val patient2 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Patient
        }

        val bundle = mockk<Bundle<Patient>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(patient1, patient2)
        }

        val oncologyPatients = transformer.transformPatients(bundle, tenant)
        assertEquals(1, oncologyPatients.size)
    }

    @Test
    fun `bundle transformation returns all when all valid`() {
        val r4Patient = R4Patient(
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
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M")
        )
        val patient1 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Patient
        }

        val patient2 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Patient
        }

        val bundle = mockk<Bundle<Patient>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(patient1, patient2)
        }

        val oncologyPatients = transformer.transformPatients(bundle, tenant)
        assertEquals(2, oncologyPatients.size)
    }
}
