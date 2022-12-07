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
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.validate.resource.R4PatientValidator
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse
import com.projectronin.interop.fhir.r4.valueset.LinkType
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.conceptmap.ConceptMapClient
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninPatientTest {
    private lateinit var conceptMapClient: ConceptMapClient
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
    private val identifierService = mockk<IdentifierService> {
        every { getMRNIdentifier(tenant, identifierList) } returns identifierList[0]
        every { getMRNIdentifier(tenant, emptyList()) } throws VendorIdentifierNotFoundException()
    }

    @BeforeEach
    fun setup() {
        conceptMapClient = mockk()
        roninPatient = RoninPatient.create(identifierService, conceptMapClient)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(roninPatient.qualifies(Patient()))
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "An MRN".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
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
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
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
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
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
                Identifier(type = CodeableConcepts.RONIN_MRN, system = CodeSystem.RONIN_MRN.uri, value = null)
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
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
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
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
                )
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
    fun `validate fails for null or empty name`() {
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
                )
            ),
            name = listOf(HumanName(family = null, given = emptyList())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
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
                )
            ),
            name = listOf(HumanName(family = "Family Name".asFHIR())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient, null).alertIfErrors()
    }

    @Test
    fun `validate pass with only given name`() {
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
                )
            ),
            name = listOf(HumanName(given = listOf("Given Name").asFHIR())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient, null).alertIfErrors()
    }

    @Test
    fun `validate pass with only multiple given names`() {
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
                )
            ),
            name = listOf(HumanName(given = listOf("Given Name", "Other Given Name").asFHIR())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient, null).alertIfErrors()
    }

    @Test
    fun `validate pass with no name and data absent reason`() {
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
                )
            ),
            name = listOf(
                HumanName(
                    extension = listOf(
                        Extension(
                            url = Uri("http://hl7.org/fhir/StructureDefinition/data-absent-reason"),
                            value = DynamicValue(DynamicValueType.CODE, Code(value = "unknown"))
                        )
                    )
                )
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
        )

        roninPatient.validate(patient, null).alertIfErrors()
    }

    @Test
    fun `validate fails with name and data absent reason`() {
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
                )
            ),
            name = listOf(
                HumanName(
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
            birthDate = Date("1975-07-05"),
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, null).alertIfErrors()
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
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
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
                Identifier(type = CodeableConcepts.RONIN_MRN, system = null, value = "missing system".asFHIR())
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
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
                Identifier(type = CodeableConcepts.RONIN_MRN, system = CodeSystem.RONIN_MRN.uri)
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
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
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            telecom = listOf(ContactPoint(value = "1234567890".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPatient.validate(patient, LocationContext(Patient::class)).alertIfErrors()
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

        val emailSystemValue = ContactPointSystem.EMAIL.code
        val emailSystemExtensionUri = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri
        val emailSystemExtensionValue = DynamicValue(
            type = DynamicValueType.CODING,
            value = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", emailSystemValue)
        )
        val emailSystem = Code(
            value = emailSystemValue,
            extension = listOf(Extension(url = emailSystemExtensionUri, value = emailSystemExtensionValue))
        )

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
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            telecom = listOf(ContactPoint(system = emailSystem))
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
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
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
    fun `validates patient with minimum attributes`() {
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
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        roninPatient.validate(patient, null).alertIfErrors()
    }

    @Test
    fun `transforms patient with all attributes`() {
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "telephone")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("phone"), systemExtension("telephone"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Patient",
                    "Patient.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "cell")
                    ),
                    ContactPointUse::class
                )
            } returns Pair(useCoding("mobile"), useExtension("cell"))
        }
        roninPatient = RoninPatient.create(identifierService, conceptMapClient)
        val patient = Patient(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/patient"))
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
            name = listOf(HumanName(family = "Doe".asFHIR())),
            telecom = listOf(
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
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            deceased = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.FALSE),
            address = listOf(Address(country = "USA".asFHIR())),
            maritalStatus = CodeableConcept(text = "M".asFHIR()),
            multipleBirth = DynamicValue(DynamicValueType.INTEGER, FHIRInteger(2)),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            contact = listOf(Contact(name = HumanName(text = "Jane Doe".asFHIR()))),
            communication = listOf(Communication(language = CodeableConcept(text = "English".asFHIR()))),
            generalPractitioner = listOf(Reference(display = "GP".asFHIR())),
            managingOrganization = Reference(display = "organization".asFHIR()),
            link = listOf(
                PatientLink(
                    other = Reference(display = "other patient".asFHIR()),
                    type = LinkType.REPLACES.asCode()
                )
            )
        )

        val oncologyPatient = roninPatient.transform(patient, tenant)
        oncologyPatient!!
        assertEquals("Patient", oncologyPatient.resourceType)
        assertEquals(Id("test-12345"), oncologyPatient.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PATIENT.value))),
            oncologyPatient.meta
        )
        assertEquals(Uri("implicit-rules"), oncologyPatient.implicitRules)
        assertEquals(Code("en-US"), oncologyPatient.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), oncologyPatient.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            oncologyPatient.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                )
            ),
            oncologyPatient.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                )
            ),
            oncologyPatient.modifierExtension
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
                )
            ),
            oncologyPatient.identifier
        )
        assertEquals(FHIRBoolean.TRUE, oncologyPatient.active)
        assertEquals(listOf(HumanName(family = "Doe".asFHIR())), oncologyPatient.name)
        assertEquals(
            listOf(
                ContactPoint(
                    id = "12345".asFHIR(),
                    extension = listOf(
                        Extension(
                            url = Uri("http://localhost/extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                        )
                    ),
                    value = "8675309".asFHIR(),
                    system = Code(
                        value = "phone",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.system"
                                            )
                                        ),
                                        code = Code(value = "telephone")
                                    )
                                )
                            )
                        )
                    ),
                    use = Code(
                        value = "mobile",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_USE.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.use"
                                            )
                                        ),
                                        code = Code(value = "cell")
                                    )
                                )
                            )
                        )
                    ),
                    rank = PositiveInt(1),
                    period = Period(start = DateTime(value = "2021-11-18"), end = DateTime(value = "2022-11-17"))
                ),
                ContactPoint(
                    value = "1112223333".asFHIR(),
                    system = Code(
                        value = "phone",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.system"
                                            )
                                        ),
                                        code = Code(value = "telephone")
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            oncologyPatient.telecom
        )
        assertEquals(AdministrativeGender.FEMALE.asCode(), oncologyPatient.gender)
        assertEquals(Date("1975-07-05"), oncologyPatient.birthDate)
        assertEquals(DynamicValue(type = DynamicValueType.BOOLEAN, value = FHIRBoolean.FALSE), oncologyPatient.deceased)
        assertEquals(listOf(Address(country = "USA".asFHIR())), oncologyPatient.address)
        assertEquals(CodeableConcept(text = "M".asFHIR()), oncologyPatient.maritalStatus)
        assertEquals(
            DynamicValue(type = DynamicValueType.INTEGER, value = FHIRInteger(2)),
            oncologyPatient.multipleBirth
        )
        assertEquals(
            listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            oncologyPatient.photo
        )
        assertEquals(
            listOf(Communication(language = CodeableConcept(text = "English".asFHIR()))),
            oncologyPatient.communication
        )
        assertEquals(listOf(Reference(display = "GP".asFHIR())), oncologyPatient.generalPractitioner)
        assertEquals(Reference(display = "organization".asFHIR()), oncologyPatient.managingOrganization)
        assertEquals(
            listOf(
                PatientLink(
                    other = Reference(display = "other patient".asFHIR()),
                    type = LinkType.REPLACES.asCode()
                )
            ),
            oncologyPatient.link
        )
    }

    @Test
    fun `transforms patient with minimum attributes`() {
        val patient = Patient(
            id = Id("12345"),
            identifier = identifierList,
            name = listOf(HumanName(family = "Doe".asFHIR())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val oncologyPatient = roninPatient.transform(patient, tenant)

        val defaultCoding = Coding(
            system = Uri("http://terminology.hl7.org/CodeSystem/v3-NullFlavor"),
            code = Code("NI"),
            display = "NoInformation".asFHIR()
        )
        oncologyPatient!!
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
                )
            ),
            oncologyPatient.identifier
        )
        assertNull(oncologyPatient.active)
        assertEquals(listOf(HumanName(family = "Doe".asFHIR())), oncologyPatient.name)
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
            name = listOf(HumanName(family = "Doe".asFHIR())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA".asFHIR())),
            maritalStatus = CodeableConcept(text = "M".asFHIR())
        )

        val oncologyPatient = roninPatient.transform(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `validate succeeds for identifier with value extension`() {
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
            name = listOf(HumanName(family = "Doe".asFHIR())),
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
        val normalizedIdentifierWithExtension = Identifier(
            use = IdentifierUse.USUAL.asCode(),
            system = Uri("http://hl7.org/fhir/sid/us-ssn"),
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
        val normalizedIdentifiers = identifierList + normalizedIdentifierWithExtension

        every { identifierService.getMRNIdentifier(tenant, normalizedIdentifiers) } returns identifiers[0]

        val patient = Patient(
            id = Id("12345"),
            identifier = identifiers,
            name = listOf(HumanName(family = "Doe".asFHIR())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val oncologyPatient = roninPatient.transform(patient, tenant)

        val defaultCoding = Coding(
            system = Uri("http://terminology.hl7.org/CodeSystem/v3-NullFlavor"),
            code = Code("NI"),
            display = "NoInformation".asFHIR()
        )
        oncologyPatient!!
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
                )
            ),
            oncologyPatient.identifier
        )
        assertNull(oncologyPatient.active)
        assertEquals(listOf(HumanName(family = "Doe".asFHIR())), oncologyPatient.name)
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

    private fun systemCoding(value: String) = Coding(
        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
        code = Code(value = value)
    )

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

    private fun useCoding(value: String) = Coding(
        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
        code = Code(value = value)
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
