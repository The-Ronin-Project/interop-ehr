package com.projectronin.interop.fhir.ronin.resource.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.ConditionEvidence
import com.projectronin.interop.fhir.r4.resource.ConditionStage
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.validate.resource.R4ConditionValidator
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninConditionEncounterDiagnosisTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val profile = RoninConditionEncounterDiagnosis(normalizer, localizer)

    @Test
    fun `does not qualify when no categories`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR())
        )

        val qualified = profile.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category with no codings`() {
        val condition = Condition(
            id = Id("12345"),
            category = listOf(CodeableConcept(text = "category".asFHIR())),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR())
        )

        val qualified = profile.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category coding code is not for encounter diagnosis`() {
        val condition = Condition(
            id = Id("12345"),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("something")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR())
        )

        val qualified = profile.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category coding code is for encounter diagnosis and wrong system`() {
        val condition = Condition(
            id = Id("12345"),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9"),
                        display = FHIRString("Malignant neoplasm of unspecified kidney except renal pelvis")
                    )
                ),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR())
        )

        val qualified = profile.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `qualifies for profile`() {
        val condition = Condition(
            id = Id("12345"),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR())
        )

        val qualified = profile.qualifies(condition)
        assertTrue(qualified)
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Condition.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Condition.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails if no code`() {
        val condition = Condition(
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
            code = null,
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: code is a required element @ Condition.code",
            exception.message
        )
    }

    @Test
    @Disabled("Coding Validation is currently disabled due to lack of mapping content.")
    fun `validate fails if code coding is empty`() {
        val condition = Condition(
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
            code = CodeableConcept(
                coding = listOf(),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Condition.code",
            exception.message
        )
    }

    @Test
    @Disabled("Coding Validation is currently disabled due to lack of mapping content.")
    fun `validate fails if code coding system is missing`() {
        val condition = Condition(
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
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Condition.code",
            exception.message
        )
    }

    @Test
    @Disabled("Coding Validation is currently disabled due to lack of mapping content.")
    fun `validate fails if code coding display is missing`() {
        val condition = Condition(
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
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9")
                    )
                ),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Condition.code",
            exception.message
        )
    }

    @Test
    @Disabled("Coding Validation is currently disabled due to lack of mapping content.")
    fun `validate fails if code coding code is missing`() {
        val condition = Condition(
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
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Condition.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if not an encounter diagnosis`() {
        val condition = Condition(
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
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            ),
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_CND_ENC_DX_001: Encounter diagnosis condition category required for US Core Condition Encounter Diagnosis profile @ Condition.category",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val condition = Condition(
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
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            )
        )

        mockkObject(R4ConditionValidator)
        every { R4ConditionValidator.validate(condition, LocationContext(Condition::class)) } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Condition::onset),
                LocationContext(Condition::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: onset is a required element @ Condition.onset",
            exception.message
        )

        unmockkObject(R4ConditionValidator)
    }

    @Test
    fun `validate succeeds`() {
        val condition = Condition(
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
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            )
        )

        profile.validate(condition, null).alertIfErrors()
    }

    @Test
    fun `transform fails for condition with no ID`() {
        val condition = Condition(
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            )
        )

        val (transformed, _) = profile.transform(condition, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transforms condition with all attributes`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/practitioner"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"13579"}""")),
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
            identifier = listOf(
                Identifier(value = FHIRString("id"))
            ),
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("inactive"),
                        display = "Inactive".asFHIR()
                    )
                )
            ),
            verificationStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                        code = Code("confirmed"),
                        display = "Confirmed".asFHIR()
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            severity = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("371924009"),
                        display = "Moderate to severe".asFHIR()
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer".asFHIR()
                    )
                )
            ),
            bodySite = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://snomed.info/sct"),
                            code = Code("39607008"),
                            display = "Lung structure (body structure)".asFHIR()
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/roninPatientExample01".asFHIR()
            ),
            encounter = Reference(
                reference = "Encounter/roninEncounterExample01".asFHIR()
            ),
            onset = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2019-04-01")),
            abatement = DynamicValue(
                DynamicValueType.PERIOD,
                Period(start = DateTime("2019-04-01"), end = DateTime("2022-04-01"))
            ),
            recordedDate = DateTime("2022-01-01"),
            recorder = Reference(
                reference = "Practitioner/roninPractitionerExample01".asFHIR()
            ),
            asserter = Reference(
                reference = "Practitioner/roninPractitionerExample01".asFHIR()
            ),
            stage = listOf(
                ConditionStage(
                    summary = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://cancerstaging.org"),
                                code = Code("3C"),
                                display = "IIIC".asFHIR()
                            )
                        )
                    )
                )
            ),
            evidence = listOf(
                ConditionEvidence(
                    detail = listOf(
                        Reference(
                            reference = "DiagnosticReport/Test01".asFHIR()
                        )
                    )
                )
            ),
            note = listOf(
                Annotation(
                    author = DynamicValue(
                        DynamicValueType.REFERENCE,
                        Reference(reference = "Practitioner/roninPractitionerExample01".asFHIR())
                    ),
                    text = Markdown("Test")
                )
            )
        )

        val (transformed, validation) = profile.transform(condition, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Condition", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS.value))),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = FHIRString("div")), transformed.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"13579"}""")),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = FHIRString("id")),
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
            transformed.identifier
        )
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("inactive"),
                        display = "Inactive".asFHIR()
                    )
                )
            ),
            transformed.clinicalStatus
        )
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                        code = Code("confirmed"),
                        display = "Confirmed".asFHIR()
                    )
                )
            ),
            transformed.verificationStatus
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            transformed.category
        )
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("371924009"),
                        display = "Moderate to severe".asFHIR()
                    )
                )
            ),
            transformed.severity
        )
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer".asFHIR()
                    )
                )
            ),
            transformed.code
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://snomed.info/sct"),
                            code = Code("39607008"),
                            display = "Lung structure (body structure)".asFHIR()
                        )
                    )
                )
            ),
            transformed.bodySite
        )
        assertEquals(
            Reference(
                reference = "Patient/roninPatientExample01".asFHIR()
            ),
            transformed.subject
        )
        assertEquals(
            Reference(
                reference = "Encounter/roninEncounterExample01".asFHIR()
            ),
            transformed.encounter
        )
        assertEquals(DynamicValue(DynamicValueType.DATE_TIME, DateTime("2019-04-01")), transformed.onset)
        assertEquals(
            DynamicValue(
                DynamicValueType.PERIOD,
                Period(start = DateTime("2019-04-01"), end = DateTime("2022-04-01"))
            ),
            transformed.abatement
        )
        assertEquals(DateTime("2022-01-01"), transformed.recordedDate)
        assertEquals(
            Reference(reference = FHIRString("Practitioner/roninPractitionerExample01")),
            transformed.recorder
        )
        assertEquals(
            Reference(reference = FHIRString("Practitioner/roninPractitionerExample01")),
            transformed.asserter
        )
        assertEquals(
            listOf(
                ConditionStage(
                    summary = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://cancerstaging.org"),
                                code = Code("3C"),
                                display = "IIIC".asFHIR()
                            )
                        )
                    )
                )
            ),
            transformed.stage
        )
        assertEquals(
            listOf(
                ConditionEvidence(
                    detail = listOf(
                        Reference(
                            reference = "DiagnosticReport/Test01".asFHIR()
                        )
                    )
                )
            ),
            transformed.evidence
        )
        assertEquals(
            listOf(
                Annotation(
                    author = DynamicValue(
                        DynamicValueType.REFERENCE,
                        Reference(reference = "Practitioner/roninPractitionerExample01".asFHIR())
                    ),
                    text = Markdown("Test")
                )
            ),
            transformed.note
        )
    }

    @Test
    fun `transforms condition with only required attributes`() {
        val condition = Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(value = "id".asFHIR())
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer".asFHIR()
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/roninPatientExample01".asFHIR()
            )
        )

        val (transformed, validation) = profile.transform(condition, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Condition", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS.value))),
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
                Identifier(value = FHIRString("id")),
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
            transformed.identifier
        )
        assertNull(transformed.clinicalStatus)
        assertNull(transformed.verificationStatus)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            transformed.category
        )
        assertNull(transformed.severity)
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer".asFHIR()
                    )
                )
            ),
            transformed.code
        )
        assertEquals(listOf<CodeableConcept>(), transformed.bodySite)
        assertEquals(
            Reference(
                reference = "Patient/roninPatientExample01".asFHIR()
            ),
            transformed.subject
        )
        assertNull(transformed.encounter)
        assertNull(transformed.onset)
        assertNull(transformed.abatement)
        assertNull(transformed.recordedDate)
        assertNull(transformed.recorder)
        assertNull(transformed.asserter)
        assertEquals(listOf<ConditionStage>(), transformed.stage)
        assertEquals(listOf<ConditionEvidence>(), transformed.evidence)
        assertEquals(listOf<Annotation>(), transformed.note)
    }

    // Note: This test may be temporary while we are waiting on concept mapping data.
    @Test
    fun `validate succeeds with partial code codings`() {
        val condition = Condition(
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
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    ),
                    Coding(
                        system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                        code = Code("C64.9")
                    ),
                    Coding(
                        code = Code("C64.9"),
                        display = "Malignant neoplasm of unspecified kidney except renal pelvis".asFHIR()
                    )
                ),
                text = "code".asFHIR()
            )
        )

        profile.validate(condition, null).alertIfErrors()
    }

    // Note: This test may be temporary while we are waiting on concept mapping data.
    @Test
    fun `validate succeeds with no code codings`() {
        val condition = Condition(
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
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                text = "code".asFHIR()
            )
        )

        profile.validate(condition, null).alertIfErrors()
    }
}
