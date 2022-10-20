package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.IdentifierService
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
import com.projectronin.interop.fhir.r4.datatype.PrimitiveData
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.validate.resource.R4PatientValidator
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse
import com.projectronin.interop.fhir.r4.valueset.LinkType
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninPatientTest {
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
    private val roninPatient: RoninPatient = RoninPatient.create(identifierService)

    @Test
    fun `always qualifies`() {
        assertTrue(roninPatient.qualifies(Patient()))
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Patient.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Patient.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails for no MRN identifier`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345")
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PAT_001: MRN identifier is required @ Patient.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails for wrong type on MRN identifier`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PAT_002: MRN identifier type defined without proper CodeableConcept @ Patient.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails for no value on MRN identifier`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = null)
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PAT_003: MRN identifier value is required @ Patient.identifier\n" +
                "ERROR REQ_FIELD: value is a required element @ Patient.identifier[2].value",
            exception.message
        )
    }

    @Test
    fun `validate fails for no birth date`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = null
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: birthDate is a required element @ Patient.birthDate",
            exception.message
        )
    }

    @Test
    fun `validate fails for no name`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            name = listOf(),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_PAT_001: At least one name must be provided @ Patient.name",
            exception.message
        )
    }

    @Test
    fun `validate fails for no gender`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = null,
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: gender is a required element @ Patient.gender",
            exception.message
        )
    }

    @Test
    fun `validate fails for missing identifier system`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN"),
                Identifier(type = RoninCodeableConcepts.MRN, system = null, value = "missing system")
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: system is a required element @ Patient.identifier[3].system",
            exception.message
        )
    }

    @Test
    fun `validate fails for missing identifier value`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri)
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: value is a required element @ Patient.identifier[3].value",
            exception.message
        )
    }

    @Test
    fun `validate fails for missing telecom system`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            telecom = listOf(ContactPoint(value = "1234567890"))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: system is a required element @ Patient.telecom[0].system\n" +
                "ERROR R4_CNTCTPT_001: A system is required if a value is provided @ Patient.telecom[0]",
            exception.message
        )
    }

    @Test
    fun `validate fails for missing telecom value`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: value is a required element @ Patient.telecom[0].value",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        mockkObject(R4PatientValidator)
        every { R4PatientValidator.validate(patient, LocationContext(Patient::class)) } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Patient::communication),
                LocationContext(Patient::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: communication is a required element @ Patient.communication",
            exception.message
        )

        unmockkObject(R4PatientValidator)
    }

    @Test
    fun `validate succeeds`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient, null).alertIfErrors()
    }

    @Test
    fun `transforms patient with all attributes`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/patient"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div"),
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
                    system = ContactPointSystem.PHONE.asCode(),
                    use = ContactPointUse.MOBILE.asCode(),
                    value = "8675309"
                )
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
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
            link = listOf(PatientLink(other = Reference(display = "other patient"), type = LinkType.REPLACES.asCode()))
        )

        val oncologyPatient = roninPatient.transform(patient, tenant)

        oncologyPatient!! // Force it to be treated as non-null
        assertEquals("Patient", oncologyPatient.resourceType)
        assertEquals(Id("test-12345"), oncologyPatient.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value))),
            oncologyPatient.meta
        )
        assertEquals(Uri("implicit-rules"), oncologyPatient.implicitRules)
        assertEquals(Code("en-US"), oncologyPatient.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div"), oncologyPatient.text)
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
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345"
                ),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            oncologyPatient.identifier
        )
        assertEquals(true, oncologyPatient.active)
        assertEquals(listOf(HumanName(family = "Doe")), oncologyPatient.name)
        assertEquals(
            listOf(
                ContactPoint(
                    value = "8675309",
                    system = ContactPointSystem.PHONE.asCode(),
                    use = ContactPointUse.MOBILE.asCode()
                )
            ),
            oncologyPatient.telecom
        )
        assertEquals(AdministrativeGender.FEMALE.asCode(), oncologyPatient.gender)
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
            listOf(PatientLink(other = Reference(display = "other patient"), type = LinkType.REPLACES.asCode())),
            oncologyPatient.link
        )
    }

    @Test
    fun `transforms patient with only required attributes`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = identifierList,
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val oncologyPatient = roninPatient.transform(patient, tenant)

        val defaultCoding = Coding(
            system = Uri("http://terminology.hl7.org/CodeSystem/v3-NullFlavor"),
            code = Code("NI"),
            display = "NoInformation"
        )
        oncologyPatient!! // Force it to be treated as non-null
        assertEquals("Patient", oncologyPatient.resourceType)
        assertEquals(Id("test-12345"), oncologyPatient.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value))),
            oncologyPatient.meta
        )
        assertNull(oncologyPatient.implicitRules)
        assertNull(oncologyPatient.language)
        assertNull(oncologyPatient.text)
        assertEquals(listOf<ContainedResource>(), oncologyPatient.contained)
        assertEquals(listOf<Extension>(), oncologyPatient.extension)
        assertEquals(listOf<Extension>(), oncologyPatient.modifierExtension)
        assertEquals(
            listOf(
                identifierList.first(),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345"
                ),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            oncologyPatient.identifier
        )
        assertNull(oncologyPatient.active)
        assertEquals(listOf(HumanName(family = "Doe")), oncologyPatient.name)
        assertEquals(emptyList<ContactPoint>(), oncologyPatient.telecom)
        assertEquals(AdministrativeGender.FEMALE.asCode(), oncologyPatient.gender)
        assertEquals(Date("1975-07-05"), oncologyPatient.birthDate)
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
                    system = ContactPointSystem.PHONE.asCode(),
                    use = ContactPointUse.MOBILE.asCode(),
                    value = "8675309"
                )
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M")
        )

        val oncologyPatient = roninPatient.transform(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `validate succeeds for identifier with value extension`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN"),
                Identifier(
                    use = IdentifierUse.USUAL.asCode(),
                    system = Uri("urn:oid:2.16.840.1.113883.4.1"),
                    valueData = PrimitiveData(
                        extension = listOf(
                            Extension(
                                url = Uri("http://hl7.org/fhir/StructureDefinition/rendered-value"),
                                value = DynamicValue(DynamicValueType.STRING, "xxx-xx-1234")
                            )
                        )
                    )
                )
            ),
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient, null).alertIfErrors()
    }

    @Test
    fun `transforms patient with value extensions`() {
        val identifierWithExtension = Identifier(
            use = IdentifierUse.USUAL.asCode(),
            system = Uri("urn:oid:2.16.840.1.113883.4.1"),
            valueData = PrimitiveData(
                extension = listOf(
                    Extension(
                        url = Uri("http://hl7.org/fhir/StructureDefinition/rendered-value"),
                        value = DynamicValue(DynamicValueType.STRING, "xxx-xx-1234")
                    )
                )
            )
        )
        val normalizedIdentifierWithExtension = Identifier(
            use = IdentifierUse.USUAL.asCode(),
            system = Uri("http://hl7.org/fhir/sid/us-ssn"),
            valueData = PrimitiveData(
                extension = listOf(
                    Extension(
                        url = Uri("http://hl7.org/fhir/StructureDefinition/rendered-value"),
                        value = DynamicValue(DynamicValueType.STRING, "xxx-xx-1234")
                    )
                )
            )
        )

        val identifiers = identifierList + identifierWithExtension
        val normalizedIdentifiers = identifierList + normalizedIdentifierWithExtension

        every { identifierService.getMRNIdentifier(tenant, normalizedIdentifiers) } returns identifiers[0]

        val patient = Patient(
            id = Id("12345"),
            identifier = identifiers,
            name = listOf(HumanName(family = "Doe")),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val oncologyPatient = roninPatient.transform(patient, tenant)

        val defaultCoding = Coding(
            system = Uri("http://terminology.hl7.org/CodeSystem/v3-NullFlavor"),
            code = Code("NI"),
            display = "NoInformation"
        )
        oncologyPatient!! // Force it to be treated as non-null
        assertEquals("Patient", oncologyPatient.resourceType)
        assertEquals(Id("test-12345"), oncologyPatient.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value))),
            oncologyPatient.meta
        )
        assertNull(oncologyPatient.implicitRules)
        assertNull(oncologyPatient.language)
        assertNull(oncologyPatient.text)
        assertEquals(listOf<ContainedResource>(), oncologyPatient.contained)
        assertEquals(listOf<Extension>(), oncologyPatient.extension)
        assertEquals(listOf<Extension>(), oncologyPatient.modifierExtension)
        assertEquals(
            listOf(
                identifierList.first(),
                normalizedIdentifierWithExtension,
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345"
                ),
                Identifier(type = RoninCodeableConcepts.MRN, system = RoninCodeSystem.MRN.uri, value = "An MRN")
            ),
            oncologyPatient.identifier
        )
        assertNull(oncologyPatient.active)
        assertEquals(listOf(HumanName(family = "Doe")), oncologyPatient.name)
        assertEquals(emptyList<ContactPoint>(), oncologyPatient.telecom)
        assertEquals(AdministrativeGender.FEMALE.asCode(), oncologyPatient.gender)
        assertEquals(Date("1975-07-05"), oncologyPatient.birthDate)
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
}
