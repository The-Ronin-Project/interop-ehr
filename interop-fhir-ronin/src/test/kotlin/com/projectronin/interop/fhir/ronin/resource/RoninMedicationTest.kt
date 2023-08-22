package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Ratio
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Batch
import com.projectronin.interop.fhir.r4.resource.Ingredient
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.valueset.MedicationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninMedicationTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val vitaminD = "11253"
    private val medicationCode = Code(vitaminD)
    private val medicationCoding =
        Coding(system = CodeSystem.RXNORM.uri, code = medicationCode, display = "Vitamin D".asFHIR())
    private val medicationCodingList = listOf(medicationCoding)
    private val roninMedication = RoninMedication(normalizer, localizer)

    private val tenantSourceCodeExtensionB = listOf(
        Extension(
            url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
            value = DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    text = "b".asFHIR(),
                    coding = medicationCodingList
                )
            )
        )
    )

    @Test
    fun `always qualifies`() {
        assertTrue(roninMedication.qualifies(Medication()))
    }

    @Test
    fun `validate - fails if missing identifiers`() {
        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedication.validate(medication).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Medication.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Medication.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ Medication.identifier",
            exception.message
        )
    }

    @Test
    fun `validate - fails if missing required code attribute`() {
        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = tenantSourceCodeExtensionB
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedication.validate(medication).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: code is a required element @ Medication.code",
            exception.message
        )
    }

    @Test
    fun `validate - fails if no code-coding has all required attributes - missing values`() {
        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                text = "k".asFHIR(),
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = medicationCode
                    ),
                    Coding(
                        system = CodeSystem.RXNORM.uri
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedication.validate(medication).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Medication.code",
            exception.message
        )
    }

    @Test
    fun `transform - succeeds - code is missing the text attribute - no userSelected coding - missing text`() {
        // except for the test case details,
        // all attributes are correct

        val specialCode = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("b"),
                    code = Code("b"),
                    version = "1.0.0".asFHIR(),
                    display = "b".asFHIR()
                ),
                Coding(
                    system = Uri("i"),
                    code = Code("i"),
                    version = "1.0.1".asFHIR(),
                    display = "i".asFHIR()
                ),
                medicationCoding
            )
        )
        val medication = Medication(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            code = specialCode
        )

        // transformation
        val (transformed, validation) = roninMedication.transform(medication, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            medication.extension + listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        specialCode
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(
            specialCode,
            transformed.code
        )
    }

    @Test
    fun `transform - succeeds - code has an empty-valued text attribute - no userSelected coding - empty-valued text`() {
        // except for the test case details,
        // all attributes are correct

        val specialCode = CodeableConcept(
            text = "".asFHIR(),
            coding = listOf(
                Coding(
                    system = Uri("b"),
                    code = Code("b"),
                    version = "1.0.0".asFHIR(),
                    display = "b".asFHIR()
                ),
                Coding(
                    system = Uri("i"),
                    code = Code("i"),
                    version = "1.0.1".asFHIR(),
                    display = "i".asFHIR()
                ),
                medicationCoding
            )
        )
        val medication = Medication(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            code = specialCode
        )

        // transformation
        val (transformed, validation) = roninMedication.transform(medication, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(
            medication.extension + listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        specialCode
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(
            specialCode,
            transformed.code
        )
    }

    @Test
    fun `validate - error - more than 1 userSelected entry`() {
        // except for the test case details,
        // all attributes are correct

        val specialCode = CodeableConcept(
            text = "".asFHIR(),
            coding = listOf(
                Coding(
                    system = CodeSystem.RXNORM.uri,
                    code = Code("b"),
                    version = "1.0.0".asFHIR(),
                    display = "b".asFHIR()
                ),
                Coding(
                    system = CodeSystem.RXNORM.uri,
                    code = medicationCode,
                    version = "1.0.0".asFHIR(),
                    display = "e".asFHIR(),
                    userSelected = FHIRBoolean.TRUE
                ),
                Coding(
                    system = CodeSystem.RXNORM.uri,
                    code = medicationCode,
                    version = "1.0.1".asFHIR(),
                    display = "i".asFHIR(),
                    userSelected = FHIRBoolean.TRUE
                )
            )
        )

        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = tenantSourceCodeExtensionB,
            code = specialCode
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedication.validate(medication).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_CODING_SEL_001: More than one coding entry has userSelected true @ Medication.code",
            exception.message
        )
    }

    @Test
    fun `validate - fails if status does not use required valueset`() {
        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            ),
            status = Code("x")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedication.validate(medication).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'x' is outside of required value set @ Medication.status",
            exception.message
        )
    }

    @Test
    fun `validate - fails for any ingredient missing an item`() {
        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            ),
            ingredient = listOf(
                Ingredient(isActive = FHIRBoolean.TRUE),
                Ingredient(
                    item = DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = "Organization/item".asFHIR())
                    ),
                    isActive = FHIRBoolean.TRUE
                ),
                Ingredient(isActive = FHIRBoolean.FALSE)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedication.validate(medication).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: item is a required element @ Medication.ingredient[0].item\n" +
                "ERROR REQ_FIELD: item is a required element @ Medication.ingredient[2].item",
            exception.message
        )
    }

    @Test
    fun `validate checks meta`() {
        val medication = Medication(
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            ),
            ingredient = listOf()
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedication.validate(medication).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ Medication.meta",
            exception.message
        )
    }

    @Test
    fun `validate - succeeds with empty ingredient list`() {
        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            ),
            ingredient = listOf()
        )

        roninMedication.validate(medication).alertIfErrors()
    }

    @Test
    fun `validate - succeeds with just required attributes`() {
        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        roninMedication.validate(medication).alertIfErrors()
    }

    @Test
    fun `transform - succeeds with just required attributes`() {
        val medication = Medication(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        val (transformed, validation) = roninMedication.transform(medication, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(Id("12345"), transformed.id)
        assertEquals(3, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        // &&& extension
        assertEquals(medication.code, transformed.code)
    }

    @Test
    fun `transform and validate - succeeds with all attributes present - ingredient item is type REFERENCE`() {
        val medication = Medication(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/Medication.html")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(Location(id = Id("67890"))),
            extension = listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "67890".asFHIR())),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            ),
            status = MedicationStatus.ACTIVE.asCode(),
            manufacturer = Reference(reference = "Organization/c".asFHIR()),
            form = CodeableConcept(
                text = "d".asFHIR(),
                coding = medicationCodingList
            ),
            amount = Ratio(
                numerator = Quantity(
                    value = Decimal(1.5),
                    unit = "mg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("mg")
                ),
                denominator = Quantity(
                    value = Decimal(1.0),
                    unit = "mg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("mg")
                )
            ),
            ingredient = listOf(
                Ingredient(
                    item = DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = "Organization/item".asFHIR())
                    ),
                    isActive = FHIRBoolean.TRUE,
                    strength = Ratio(
                        numerator = Quantity(
                            value = Decimal(0.5),
                            unit = "mg".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mg")
                        ),
                        denominator = Quantity(
                            value = Decimal(1.0),
                            unit = "mg".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mg")
                        )
                    )
                )
            ),
            batch = Batch(
                lotNumber = "e".asFHIR(),
                expirationDate = DateTime("2022-10-14")
            )
        )

        // transformation
        val (transformed, validation) = roninMedication.transform(medication, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION.value,
            transformed.meta!!.profile[0].value
        )
        assertEquals(medication.implicitRules, transformed.implicitRules)
        assertEquals(medication.language, transformed.language)
        assertEquals(medication.text, transformed.text)
        assertEquals(medication.contained, transformed.contained)
        assertEquals(
            medication.extension + tenantSourceCodeExtensionB,
            transformed.extension
        )
        assertEquals(medication.modifierExtension, transformed.modifierExtension)
        assertEquals(4, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(value = "67890".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(medication.code, transformed.code)
        assertEquals(Code(value = "active"), transformed.status)
        assertEquals(Reference(reference = "Organization/c".asFHIR()), transformed.manufacturer)
        assertEquals(medication.form, transformed.form)
        assertEquals(medication.amount, transformed.amount)
        assertEquals(DynamicValueType.REFERENCE, transformed.ingredient[0].item?.type)
        assertEquals(
            Reference(reference = "Organization/item".asFHIR()),
            transformed.ingredient[0].item?.value
        )
        assertEquals(medication.batch, transformed.batch)

        // validation
        roninMedication.validate(transformed).alertIfErrors()
    }

    @Test
    fun `transform and validate - succeeds with all attributes present - ingredient item is type CODEABLE_CONCEPT`() {
        val medication = Medication(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/Medication.html")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(Location(id = Id("67890"))),
            extension = listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "67890".asFHIR())),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            ),
            status = MedicationStatus.ACTIVE.asCode(),
            manufacturer = Reference(reference = "Organization/c".asFHIR()),
            form = CodeableConcept(
                text = "d".asFHIR(),
                coding = medicationCodingList
            ),
            amount = Ratio(
                numerator = Quantity(
                    value = Decimal(1.5),
                    unit = "mg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("mg")
                ),
                denominator = Quantity(
                    value = Decimal(1.0),
                    unit = "mg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("mg")
                )
            ),
            ingredient = listOf(
                Ingredient(
                    item = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "f".asFHIR(),
                            coding = medicationCodingList
                        )
                    ),
                    isActive = FHIRBoolean.TRUE,
                    strength = Ratio(
                        numerator = Quantity(
                            value = Decimal(0.5),
                            unit = "mg".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mg")
                        ),
                        denominator = Quantity(
                            value = Decimal(1.0),
                            unit = "mg".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mg")
                        )
                    )
                )
            ),
            batch = Batch(
                lotNumber = "e".asFHIR(),
                expirationDate = DateTime("2022-10-14")
            )
        )

        // transformation
        val (transformed, validation) = roninMedication.transform(medication, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION.value,
            transformed.meta!!.profile[0].value
        )
        assertEquals(medication.implicitRules, transformed.implicitRules)
        assertEquals(medication.language, transformed.language)
        assertEquals(medication.text, transformed.text)
        assertEquals(medication.contained, transformed.contained)
        assertEquals(
            medication.extension + tenantSourceCodeExtensionB,
            transformed.extension
        )
        assertEquals(medication.modifierExtension, transformed.modifierExtension)
        assertEquals(4, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(value = "67890".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(medication.code, transformed.code)
        assertEquals(Code(value = "active"), transformed.status)
        assertEquals(Reference(reference = "Organization/c".asFHIR()), transformed.manufacturer)
        assertEquals(medication.form, transformed.form)
        assertEquals(medication.amount, transformed.amount)
        assertEquals(medication.ingredient, transformed.ingredient)
        assertEquals(medication.ingredient, transformed.ingredient)
        assertEquals(medication.batch, transformed.batch)

        // validation
        roninMedication.validate(transformed).alertIfErrors()
    }

    @Test
    fun `transform - returns null if validation fails - for example a required attribute is missing`() {
        val medication = Medication(
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            )
            // required code attribute is missing
        )
        val (transformed, _) = roninMedication.transform(medication, tenant)
        assertNull(transformed)
    }

    @Test
    fun `validate - passes with any code value`() {
        // except for the test case details,
        // all attributes are correct

        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    ),
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("x"),
                        version = "1.0.0".asFHIR(),
                        display = "e".asFHIR(),
                        userSelected = FHIRBoolean.TRUE
                    ),
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("y"),
                        version = "1.0.1".asFHIR(),
                        display = "i".asFHIR()
                    )
                ),
                text = "test".asFHIR()
            )
        )

        roninMedication.validate(medication).alertIfErrors()
    }

    @Test
    fun `validate - fails if missing required source code extension`() {
        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedication.validate(medication).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_MED_001: Tenant source medication code extension is missing or invalid @ Medication.extension",
            exception.message
        )
    }

    @Test
    fun `validate - fails if source code extension has wrong url`() {
        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_CONDITION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "b".asFHIR(),
                            coding = medicationCodingList
                        )
                    )
                )
            ),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedication.validate(medication).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_MED_001: Tenant source medication code extension is missing or invalid @ Medication.extension",
            exception.message
        )
    }

    @Test
    fun `validate - fails if source code extension has wrong datatype`() {
        val medication = Medication(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODING,
                        medicationCoding
                    )
                )
            ),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedication.validate(medication).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_MED_001: Tenant source medication code extension is missing or invalid @ Medication.extension",
            exception.message
        )
    }
}
