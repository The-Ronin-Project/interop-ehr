package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.factory.EHRFactory
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRInteger
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Communication
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.PatientCommunication
import com.projectronin.interop.fhir.r4.resource.PatientContact
import com.projectronin.interop.fhir.r4.resource.PatientLink
import com.projectronin.interop.fhir.r4.validate.resource.R4PatientValidator
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse
import com.projectronin.interop.fhir.r4.valueset.LinkType
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.element.RoninContactPoint
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAbsentReasonExtension
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninPatientTest {
    private lateinit var roninContactPoint: RoninContactPoint
    private lateinit var normalizer: Normalizer
    private lateinit var localizer: Localizer
    private lateinit var roninPatient: RoninPatient

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val identifierList = listOf(
        Identifier(
            type = CodeableConcept(
                text = "MRN".asFHIR()
            ),
            system = Uri("mrnSystem"),
            value = "An MRN".asFHIR()
        )
    )
    private val mockIdentifierService = mockk<IdentifierService> {
        every { getMRNIdentifier(tenant, identifierList) } returns identifierList[0]
        every { getMRNIdentifier(tenant, emptyList()) } throws VendorIdentifierNotFoundException()
    }

    @BeforeEach
    fun setup() {
        roninContactPoint = mockk {
            every { validateRonin(any(), LocationContext(Patient::class), any()) } answers { thirdArg() }
            every { validateUSCore(any(), LocationContext(Patient::class), any()) } answers { thirdArg() }
        }
        normalizer = mockk {
            every { normalize(any(), tenant) } answers { firstArg() }
        }
        localizer = mockk {
            every { localize(any(), tenant) } answers { firstArg() }
        }
        val ehrFactory = mockk<EHRFactory> {
            every { getVendorFactory(tenant) } returns mockk {
                every { identifierService } returns mockIdentifierService
            }
        }
        roninPatient = RoninPatient(ehrFactory, roninContactPoint, normalizer, localizer)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(roninPatient.qualifies(Patient()))
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Patient.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Patient.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ Patient.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails for no MRN identifier`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                ),
                Identifier(type = CodeableConcepts.RONIN_MRN, system = CodeSystem.RONIN_MRN.uri, value = null)
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PAT_003: MRN identifier value is required @ Patient.identifier\n" +
                "ERROR REQ_FIELD: value is a required element @ Patient.identifier[3].value",
            exception.message
        )
    }

    @Test
    fun `validate fails for no birth date`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = null
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PAT_005: A name for official use must be present @ Patient.name\n" +
                "ERROR USCORE_PAT_001: At least one name must be provided @ Patient.name",
            exception.message
        )
    }

    @Test
    fun `validate fails for null or empty name`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = null, given = emptyList(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_PAT_002: Either Patient.name.given and/or Patient.name.family SHALL be present " +
                "or a Data Absent Reason Extension SHALL be present. @ Patient.name[0].name",
            exception.message
        )
    }

    @Test
    fun `validate pass with only family name`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Family Name".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient).alertIfErrors()
    }

    @Test
    fun `validate pass with only given name`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(given = listOf("Given Name").asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient).alertIfErrors()
    }

    @Test
    fun `validate pass with only multiple given names`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(given = listOf("Given Name", "Other Given Name").asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient).alertIfErrors()
    }

    @Test
    fun `validate pass with no name and data absent reason`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(
                HumanName(
                    use = Code("official"),
                    extension = listOf(
                        Extension(
                            url = Uri("http://hl7.org/fhir/StructureDefinition/data-absent-reason"),
                            value = DynamicValue(DynamicValueType.CODE, Code(value = "unknown"))
                        )
                    )
                )
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient).alertIfErrors()
    }

    @Test
    fun `validate fails with name and data absent reason`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(
                HumanName(
                    use = Code("official"),
                    family = "Family Name".asFHIR(),
                    extension = listOf(
                        Extension(
                            url = Uri("http://hl7.org/fhir/StructureDefinition/data-absent-reason"),
                            value = DynamicValue(DynamicValueType.CODE, Code(value = "asked-declined"))
                        )
                    )
                )
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_PAT_002: Either Patient.name.given and/or Patient.name.family SHALL be present " +
                "or a Data Absent Reason Extension SHALL be present. @ Patient.name[0].name",
            exception.message
        )
    }

    @Test
    fun `validate fails for no gender`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = null,
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: gender is a required element @ Patient.gender",
            exception.message
        )
    }

    @Test
    fun `validate fails for gender with data absent reason`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = Code(
                value = null,
                extension = listOf(Extension(url = Uri("http://hl7.org/fhir/StructureDefinition/data-absent-reason")))
            ),
            birthDate = Date("1975-07-05")
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'null' is outside of required value set @ Patient.gender",
            exception.message
        )
    }

    @Test
    fun `validate fails for missing identifier system`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                ),
                Identifier(system = null, value = "missing system".asFHIR())
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PAT_006: Identifier system or data absent reason is required @ Patient.identifier[4].system",
            exception.message
        )
    }

    @Test
    fun `validate fails for identifier system with no value`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                ),
                Identifier(system = Uri(null), value = "missing system".asFHIR())
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PAT_006: Identifier system or data absent reason is required @ Patient.identifier[4].system",
            exception.message
        )
    }

    @Test
    fun `validate fails for missing identifier value`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                ),
                Identifier(type = CodeableConcepts.RONIN_MRN, system = CodeSystem.RONIN_MRN.uri)
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: value is a required element @ Patient.identifier[4].value",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
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
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: communication is a required element @ Patient.communication",
            exception.message
        )

        unmockkObject(R4PatientValidator)
    }

    @Test
    fun `validates patient with minimum attributes`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient).alertIfErrors()
    }

    @Test
    fun `transforms patient with all attributes`() {
        val telecom = listOf(
            ContactPoint(
                id = "12345".asFHIR(),
                extension = listOf(
                    Extension(
                        url = Uri("http://localhost/extension"),
                        value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                    )
                ),
                system = Code(value = "telephone"),
                use = Code(value = "cell"),
                value = "8675309".asFHIR(),
                rank = PositiveInt(1),
                period = Period(
                    start = DateTime("2021-11-18"),
                    end = DateTime("2022-11-17")
                )
            ),
            ContactPoint(
                system = Code("telephone"),
                value = "1112223333".asFHIR()
            )
        )
        val transformedTelecom = listOf(
            ContactPoint(
                id = "12345".asFHIR(),
                extension = listOf(
                    Extension(
                        url = Uri("http://localhost/extension"),
                        value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                    )
                ),
                system = Code(value = "phone", extension = listOf(systemExtension("telephone"))),
                use = Code(value = "mobile", extension = listOf(useExtension("cell"))),
                value = "8675309".asFHIR(),
                rank = PositiveInt(1),
                period = Period(
                    start = DateTime("2021-11-18"),
                    end = DateTime("2022-11-17")
                )
            ),
            ContactPoint(
                system = Code(value = "phone", extension = listOf(systemExtension("telephone"))),
                value = "1112223333".asFHIR()
            )
        )

        every { roninContactPoint.transform(telecom, tenant, LocationContext(Patient::class), any()) } returns Pair(
            transformedTelecom,
            Validation()
        )

        val patient = Patient(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/patient")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                )
            ),
            identifier = identifierList,
            active = FHIRBoolean.TRUE,
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            telecom = telecom,
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            deceased = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.FALSE),
            address = listOf(Address(country = "USA".asFHIR())),
            maritalStatus = CodeableConcept(text = "M".asFHIR()),
            multipleBirth = DynamicValue(DynamicValueType.INTEGER, FHIRInteger(2)),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            contact = listOf(PatientContact(name = HumanName(text = "Jane Doe".asFHIR()))),
            communication = listOf(PatientCommunication(language = CodeableConcept(text = "English".asFHIR()))),
            generalPractitioner = listOf(Reference(display = "GP".asFHIR())),
            managingOrganization = Reference(display = "organization".asFHIR()),
            link = listOf(
                PatientLink(
                    other = Reference(display = "other patient".asFHIR()),
                    type = LinkType.REPLACES.asCode()
                )
            )
        )

        val (transformed, validation) = roninPatient.transform(patient, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Patient", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                )
            ),
            transformed.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                )
            ),
            transformed.modifierExtension
        )
        assertEquals(
            listOf(
                identifierList.first(),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(FHIRBoolean.TRUE, transformed.active)
        assertEquals(listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))), transformed.name)
        assertEquals(transformedTelecom, transformed.telecom)
        assertEquals(AdministrativeGender.FEMALE.asCode(), transformed.gender)
        assertEquals(Date("1975-07-05"), transformed.birthDate)
        assertEquals(DynamicValue(type = DynamicValueType.BOOLEAN, value = FHIRBoolean.FALSE), transformed.deceased)
        assertEquals(listOf(Address(country = "USA".asFHIR())), transformed.address)
        assertEquals(CodeableConcept(text = "M".asFHIR()), transformed.maritalStatus)
        assertEquals(
            DynamicValue(type = DynamicValueType.INTEGER, value = FHIRInteger(2)),
            transformed.multipleBirth
        )
        assertEquals(
            listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            transformed.photo
        )
        assertEquals(
            listOf(PatientCommunication(language = CodeableConcept(text = "English".asFHIR()))),
            transformed.communication
        )
        assertEquals(listOf(Reference(display = "GP".asFHIR())), transformed.generalPractitioner)
        assertEquals(Reference(display = "organization".asFHIR()), transformed.managingOrganization)
        assertEquals(
            listOf(
                PatientLink(
                    other = Reference(display = "other patient".asFHIR()),
                    type = LinkType.REPLACES.asCode()
                )
            ),
            transformed.link
        )
    }

    @Test
    fun `transform adds data absent reason extension when identifier does not have a system value`() {
        val identifiers = identifierList + Identifier(system = null, value = "something".asFHIR())
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(source = Uri("fake-source-fake-url")),
            identifier = identifiers,
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        every { mockIdentifierService.getMRNIdentifier(tenant, identifiers) } returns identifierList[0]

        val (transformed, validation) = roninPatient.transform(patient, tenant)
        validation.alertIfErrors()
        transformed!!
        assertEquals(
            listOf(
                identifierList.first(),
                Identifier(
                    system = Uri(value = null, extension = dataAbsentReasonExtension),
                    value = "something".asFHIR(),
                    id = null,
                    extension = emptyList()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
    }

    @Test
    fun `transforms patient with minimum attributes`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            identifier = identifierList,
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val (transformed, validation) = roninPatient.transform(patient, tenant)
        validation.alertIfErrors()

        val defaultCoding = Coding(
            system = CodeSystem.NULL_FLAVOR.uri,
            code = Code("NI"),
            display = "NoInformation".asFHIR()
        )
        transformed!!
        assertEquals("Patient", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                identifierList.first(),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertNull(transformed.active)
        assertEquals(listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))), transformed.name)
        assertEquals(emptyList<ContactPoint>(), transformed.telecom)
        assertEquals(AdministrativeGender.FEMALE.asCode(), transformed.gender)
        assertEquals(Date("1975-07-05"), transformed.birthDate)
        assertNull(transformed.deceased)
        assertEquals(emptyList<Address>(), transformed.address)
        assertEquals(listOf(defaultCoding), transformed.maritalStatus?.coding)
        assertNull(transformed.multipleBirth)
        assertEquals(listOf<Attachment>(), transformed.photo)
        assertEquals(listOf<Communication>(), transformed.communication)
        assertEquals(listOf<Reference>(), transformed.generalPractitioner)
        assertNull(transformed.managingOrganization)
        assertEquals(listOf<PatientLink>(), transformed.link)
    }

    @Test
    fun `transform fails for patient with missing id`() {
        val patient = Patient(
            identifier = identifierList,
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA".asFHIR())),
            maritalStatus = CodeableConcept(text = "M".asFHIR())
        )

        val (transformed, _) = roninPatient.transform(patient, tenant)
        assertNull(transformed)
    }

    @Test
    fun `validate checks meta`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                ),
                Identifier(
                    use = IdentifierUse.USUAL.asCode(),
                    system = Uri("urn:oid:2.16.840.1.113883.4.1"),
                    value = FHIRString(
                        value = null,
                        extension = listOf(
                            Extension(
                                url = Uri("http://hl7.org/fhir/StructureDefinition/rendered-value"),
                                value = DynamicValue(DynamicValueType.STRING, "xxx-xx-1234".asFHIR())
                            )
                        )
                    )
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ Patient.meta",
            exception.message
        )
    }

    @Test
    fun `validate succeeds for identifier with value extension`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                ),
                Identifier(
                    use = IdentifierUse.USUAL.asCode(),
                    system = Uri("urn:oid:2.16.840.1.113883.4.1"),
                    value = FHIRString(
                        value = null,
                        extension = listOf(
                            Extension(
                                url = Uri("http://hl7.org/fhir/StructureDefinition/rendered-value"),
                                value = DynamicValue(DynamicValueType.STRING, "xxx-xx-1234".asFHIR())
                            )
                        )
                    )
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient).alertIfErrors()
    }

    @Test
    fun `transforms patient with value extensions`() {
        val identifierWithExtension = Identifier(
            use = IdentifierUse.USUAL.asCode(),
            system = Uri("urn:oid:2.16.840.1.113883.4.1"),
            value = FHIRString(
                value = null,
                extension = listOf(
                    Extension(
                        url = Uri("http://hl7.org/fhir/StructureDefinition/rendered-value"),
                        value = DynamicValue(DynamicValueType.STRING, "xxx-xx-1234".asFHIR())
                    )
                )
            )
        )

        val identifiers = identifierList + identifierWithExtension

        every { mockIdentifierService.getMRNIdentifier(tenant, identifiers) } returns identifiers[0]

        val patient = Patient(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            identifier = identifiers,
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val (transformed, validation) = roninPatient.transform(patient, tenant)
        validation.alertIfErrors()

        val defaultCoding = Coding(
            system = CodeSystem.NULL_FLAVOR.uri,
            code = Code("NI"),
            display = "NoInformation".asFHIR()
        )
        transformed!!
        assertEquals("Patient", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                identifierList.first(),
                identifierWithExtension,
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertNull(transformed.active)
        assertEquals(listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))), transformed.name)
        assertEquals(emptyList<ContactPoint>(), transformed.telecom)
        assertEquals(AdministrativeGender.FEMALE.asCode(), transformed.gender)
        assertEquals(Date("1975-07-05"), transformed.birthDate)
        assertNull(transformed.deceased)
        assertEquals(emptyList<Address>(), transformed.address)
        assertEquals(listOf(defaultCoding), transformed.maritalStatus?.coding)
        assertNull(transformed.multipleBirth)
        assertEquals(listOf<Attachment>(), transformed.photo)
        assertEquals(listOf<Communication>(), transformed.communication)
        assertEquals(listOf<Reference>(), transformed.generalPractitioner)
        assertNull(transformed.managingOrganization)
        assertEquals(listOf<PatientLink>(), transformed.link)
    }

    @Test
    fun `validate passes if 1 of many names has official use`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(
                HumanName(family = "Day".asFHIR()),
                HumanName(family = "Doe".asFHIR(), use = Code("official")),
                HumanName(family = "Dee".asFHIR())
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient).alertIfErrors()
    }

    @Test
    fun `validate fails if no name has official use`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(
                HumanName(family = "Day".asFHIR()),
                HumanName(family = "Doe".asFHIR()),
                HumanName(family = "Dee".asFHIR())
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PAT_005: A name for official use must be present @ Patient.name",
            exception.message
        )
    }

    @Test
    fun `validate fails for invalid birth date`() {
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("2020")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PAT_004: Birth date is invalid @ Patient.birthDate",
            exception.message
        )
    }

    private fun systemExtension(value: String) = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomSystem"),
        value = DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                code = Code(value = value)
            )
        )
    )

    private fun useExtension(value: String) = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomUse"),
        value = DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                code = Code(value = value)
            )
        )
    )
}
