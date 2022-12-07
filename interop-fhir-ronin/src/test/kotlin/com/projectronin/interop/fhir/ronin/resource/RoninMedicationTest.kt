package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Batch
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Ingredient
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
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.valueset.MedicationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
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

    @Test
    fun `always qualifies`() {
        assertTrue(RoninMedication.qualifies(Medication()))
    }

    @Test
    fun `validate - fails if missing identifiers`() {
        val medication = Medication(
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninMedication.validate(medication, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Medication.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Medication.identifier",
            exception.message
        )
    }

    @Test
    fun `validate - fails if missing required code attribute`() {
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
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninMedication.validate(medication, null).alertIfErrors()
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
                )
            ),
            code = CodeableConcept(
                text = "k".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("b")
                    ),
                    Coding(
                        system = CodeSystem.RXNORM.uri
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninMedication.validate(medication, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Medication.code",
            exception.message
        )
    }

    @Test
    fun `transform and validate - succeeds - code is missing the text attribute - no userSelected coding - missing text`() {
        // except for the test case details,
        // all attributes are correct

        val medication = Medication(
            id = Id("12345"),
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
                )
            ),
            code = CodeableConcept(
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
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("z"),
                        version = "1.0.0".asFHIR(),
                        display = "z".asFHIR()
                    )

                )
            )
        )

        // transformation
        val roninMedication = RoninMedication.transform(medication, tenant)
        roninMedication!!
        assertEquals(
            medication.extension + Extension(
                url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                value = DynamicValue(
                    DynamicValueType.CODEABLE_CONCEPT,
                    medication.code!!
                )
            ),
            roninMedication.extension
        )
        assertEquals(
            CodeableConcept(
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
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("z"),
                        version = "1.0.0".asFHIR(),
                        display = "z".asFHIR()
                    )
                )
            ),
            roninMedication.code
        )

        // validation
        RoninMedication.validate(roninMedication, null).alertIfErrors()
    }

    @Test
    fun `transform and validate - succeeds - code has an empty-valued text attribute - no userSelected coding - empty-valued text`() {
        // except for the test case details,
        // all attributes are correct

        val medication = Medication(
            id = Id("12345"),
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
                )
            ),
            code = CodeableConcept(
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
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("z"),
                        version = "1.0.0".asFHIR(),
                        display = "z".asFHIR()
                    )
                )
            )
        )

        // transformation
        val roninMedication = RoninMedication.transform(medication, tenant)
        roninMedication!!
        assertEquals(
            medication.extension + Extension(
                url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                value = DynamicValue(
                    DynamicValueType.CODEABLE_CONCEPT,
                    medication.code!!
                )
            ),
            roninMedication.extension
        )
        assertEquals(
            CodeableConcept(
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
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("z"),
                        version = "1.0.0".asFHIR(),
                        display = "z".asFHIR()
                    )
                )
            ),
            roninMedication.code
        )

        // validation
        RoninMedication.validate(roninMedication, null).alertIfErrors()
    }

    @Test
    fun `transform and validate - succeeds - text is assigned from single coding entry - no userSelected coding`() {
        // except for the test case details,
        // all attributes are correct

        val medication = Medication(
            id = Id("12345"),
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
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("b"),
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            )
        )

        // transformation
        val roninMedication = RoninMedication.transform(medication, tenant)
        roninMedication!!
        assertEquals(
            medication.extension + Extension(
                url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                value = DynamicValue(
                    DynamicValueType.CODEABLE_CONCEPT,
                    CodeableConcept(
                        text = "b".asFHIR(),
                        coding = listOf(
                            Coding(
                                system = Uri("b"),
                                code = Code("b"),
                                version = "1.0.0".asFHIR(),
                                display = "b".asFHIR()
                            )
                        )
                    )
                )
            ),
            roninMedication.extension
        )
        assertEquals(
            CodeableConcept(
                text = "b".asFHIR(),
                coding = listOf(
                    Coding(
                        system = Uri("b"),
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            ),
            roninMedication.code
        )

        // validation
        RoninMedication.validate(roninMedication, null).alertIfErrors()
    }

    @Test
    fun `transform and validate - succeeds - text is assigned from userSelected entry - 1 userSelected coding`() {
        // except for the test case details,
        // all attributes are correct

        val medication = Medication(
            id = Id("12345"),
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
                )
            ),
            code = CodeableConcept(
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
                        display = "i".asFHIR(),
                        userSelected = FHIRBoolean.TRUE
                    ),
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("z"),
                        version = "1.0.0".asFHIR(),
                        display = "z".asFHIR()
                    )
                )
            )
        )

        // transformation
        val roninMedication = RoninMedication.transform(medication, tenant)
        roninMedication!!
        assertEquals(
            medication.extension + Extension(
                url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                value = DynamicValue(
                    DynamicValueType.CODEABLE_CONCEPT,
                    CodeableConcept(
                        text = "i".asFHIR(),
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
                                display = "i".asFHIR(),
                                userSelected = FHIRBoolean.TRUE
                            ),
                            Coding(
                                system = CodeSystem.RXNORM.uri,
                                code = Code("z"),
                                version = "1.0.0".asFHIR(),
                                display = "z".asFHIR()
                            )
                        )
                    )
                )
            ),
            roninMedication.extension
        )
        assertEquals(
            CodeableConcept(
                text = "i".asFHIR(),
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
                        display = "i".asFHIR(),
                        userSelected = FHIRBoolean.TRUE
                    ),
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("z"),
                        version = "1.0.0".asFHIR(),
                        display = "z".asFHIR()
                    )
                )
            ),
            roninMedication.code
        )

        // validation
        RoninMedication.validate(roninMedication, null).alertIfErrors()
    }

    @Test
    fun `validate - error - more than 1 userSelected entry`() {
        // except for the test case details,
        // all attributes are correct

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
                )
            ),
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
                        code = Code("e"),
                        version = "1.0.0".asFHIR(),
                        display = "e".asFHIR(),
                        userSelected = FHIRBoolean.TRUE
                    ),
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("i"),
                        version = "1.0.1".asFHIR(),
                        display = "i".asFHIR(),
                        userSelected = FHIRBoolean.TRUE
                    ),
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninMedication.validate(medication, null).alertIfErrors()
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
                )
            ),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            ),
            status = Code("x")
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninMedication.validate(medication, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: status is outside of required value set @ Medication.status",
            exception.message
        )
    }

    @Test
    fun `validate - fails for any ingredient missing an item`() {
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
                )
            ),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            ),
            ingredient = listOf(
                Ingredient(isActive = FHIRBoolean.TRUE),
                Ingredient(
                    item = DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = "Organization/item".asFHIR()),
                    ),
                    isActive = FHIRBoolean.TRUE
                ),
                Ingredient(isActive = FHIRBoolean.FALSE),
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninMedication.validate(medication, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: item is a required element @ Medication.ingredient[0].item\n" +
                "ERROR REQ_FIELD: item is a required element @ Medication.ingredient[2].item",
            exception.message
        )
    }

    @Test
    fun `validate - succeeds with empty ingredient list`() {
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
                )
            ),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            ),
            ingredient = listOf()
        )

        RoninMedication.validate(medication, null).alertIfErrors()
    }

    @Test
    fun `validate - succeeds with just required attributes`() {
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
                )
            ),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            )
        )

        RoninMedication.validate(medication, null).alertIfErrors()
    }

    @Test
    fun `transform - succeeds with just required attributes`() {
        val medication = Medication(
            id = Id("12345"),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            )
        )

        val roninMedication = RoninMedication.transform(medication, tenant)

        roninMedication!!
        assertEquals(Id("${tenant.mnemonic}-12345"), roninMedication.id)
        assertEquals(2, roninMedication.identifier.size)
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
                )
            ),
            roninMedication.identifier
        )
        assertEquals(medication.code, roninMedication.code)
    }

    @Test
    fun `transform and validate - succeeds with all attributes present - ingredient item is type REFERENCE`() {
        val medication = Medication(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/Medication.html"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
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
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            ),
            status = MedicationStatus.ACTIVE.asCode(),
            manufacturer = Reference(reference = "Organization/c".asFHIR()),
            form = CodeableConcept(
                text = "d".asFHIR(),
                coding = listOf(
                    Coding(
                        system = Uri("d"),
                        code = Code("d"),
                        version = "1.0.0".asFHIR(),
                        display = "d".asFHIR()
                    )
                )
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
                        value = Reference(reference = "Organization/item".asFHIR()),
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
            ),
        )

        // transformation
        val roninMedication = RoninMedication.transform(medication, tenant)
        roninMedication!!
        assertEquals(Id("test-12345"), roninMedication.id)
        assertEquals(
            RoninProfile.MEDICATION.value,
            roninMedication.meta!!.profile[0].value
        )
        assertEquals(medication.implicitRules, roninMedication.implicitRules)
        assertEquals(medication.language, roninMedication.language)
        assertEquals(medication.text, roninMedication.text)
        assertEquals(medication.contained, roninMedication.contained)
        assertEquals(
            (
                medication.extension +
                    Extension(
                        url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                        value = DynamicValue(
                            DynamicValueType.CODEABLE_CONCEPT,
                            CodeableConcept(
                                text = "b".asFHIR(),
                                coding = listOf(
                                    Coding(
                                        system = CodeSystem.RXNORM.uri,
                                        code = Code("b"),
                                        version = "1.0.0".asFHIR(),
                                        display = "b".asFHIR()
                                    )
                                )
                            )
                        )
                    )
                ),
            roninMedication.extension
        )
        assertEquals(medication.modifierExtension, roninMedication.modifierExtension)
        assertEquals(3, roninMedication.identifier.size)
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
                )
            ),
            roninMedication.identifier
        )
        assertEquals(medication.code, roninMedication.code)
        assertEquals(Code(value = "active"), roninMedication.status)
        assertEquals(Reference(reference = "Organization/test-c".asFHIR()), roninMedication.manufacturer)
        assertEquals(medication.form, roninMedication.form)
        assertEquals(medication.amount, roninMedication.amount)
        assertEquals(DynamicValueType.REFERENCE, roninMedication.ingredient[0].item?.type)
        assertEquals(
            Reference(reference = "Organization/test-item".asFHIR()),
            roninMedication.ingredient[0].item?.value
        )
        assertEquals(medication.batch, roninMedication.batch)

        // validation
        RoninMedication.validate(roninMedication, null).alertIfErrors()
    }

    @Test
    fun `transform and validate - succeeds with all attributes present - ingredient item is type CODEABLE_CONCEPT`() {
        val medication = Medication(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/Medication.html"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
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
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            ),
            status = MedicationStatus.ACTIVE.asCode(),
            manufacturer = Reference(reference = "Organization/c".asFHIR()),
            form = CodeableConcept(
                text = "d".asFHIR(),
                coding = listOf(
                    Coding(
                        system = Uri("d"),
                        code = Code("d"),
                        version = "1.0.0".asFHIR(),
                        display = "d".asFHIR()
                    )
                )
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
                            coding = listOf(
                                Coding(
                                    system = Uri("f"),
                                    code = Code("f"),
                                    version = "1.0.0".asFHIR(),
                                    display = "f".asFHIR()
                                )
                            )
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
            ),
        )

        // transformation
        val roninMedication = RoninMedication.transform(medication, tenant)
        roninMedication!!
        assertEquals(Id("test-12345"), roninMedication.id)
        assertEquals(
            RoninProfile.MEDICATION.value,
            roninMedication.meta!!.profile[0].value
        )
        assertEquals(medication.implicitRules, roninMedication.implicitRules)
        assertEquals(medication.language, roninMedication.language)
        assertEquals(medication.text, roninMedication.text)
        assertEquals(medication.contained, roninMedication.contained)
        assertEquals(
            (
                medication.extension +
                    Extension(
                        url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                        value = DynamicValue(
                            DynamicValueType.CODEABLE_CONCEPT,
                            CodeableConcept(
                                text = "b".asFHIR(),
                                coding = listOf(
                                    Coding(
                                        system = CodeSystem.RXNORM.uri,
                                        code = Code("b"),
                                        version = "1.0.0".asFHIR(),
                                        display = "b".asFHIR()
                                    )
                                )
                            )
                        )
                    )
                ),
            roninMedication.extension
        )
        assertEquals(medication.modifierExtension, roninMedication.modifierExtension)
        assertEquals(3, roninMedication.identifier.size)
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
                )
            ),
            roninMedication.identifier
        )
        assertEquals(medication.code, roninMedication.code)
        assertEquals(Code(value = "active"), roninMedication.status)
        assertEquals(Reference(reference = "Organization/test-c".asFHIR()), roninMedication.manufacturer)
        assertEquals(medication.form, roninMedication.form)
        assertEquals(medication.amount, roninMedication.amount)
        assertEquals(medication.ingredient, roninMedication.ingredient)
        assertEquals(medication.ingredient, roninMedication.ingredient)
        assertEquals(medication.batch, roninMedication.batch)

        // validation
        RoninMedication.validate(roninMedication, null).alertIfErrors()
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
                )
            ),
            // required code attribute is missing
        )
        val roninMedication = RoninMedication.transform(medication, tenant)
        assertNull(roninMedication)
    }
}
