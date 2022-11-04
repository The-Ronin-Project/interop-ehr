package com.projectronin.interop.fhir.ronin.resource.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ConditionEvidence
import com.projectronin.interop.fhir.r4.datatype.ConditionStage
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
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.validate.resource.R4ConditionValidator
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninConditionProblemsAndHealthConcernsTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `does not qualify when no categories`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference")
        )

        val qualified = RoninConditionProblemsAndHealthConcerns.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category with no codings`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference"),
            category = listOf(CodeableConcept(text = "category"))
        )

        val qualified = RoninConditionProblemsAndHealthConcerns.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when coding code is not for a qualifying value`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference"),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("something")
                        )
                    )
                )
            )
        )

        val qualified = RoninConditionProblemsAndHealthConcerns.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when coding code is for qualifying code and wrong system`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference"),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val qualified = RoninConditionProblemsAndHealthConcerns.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `qualifies for problem list item`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference"),
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

        val qualified = RoninConditionProblemsAndHealthConcerns.qualifies(condition)
        assertTrue(qualified)
    }

    @Test
    fun `qualifies for health concern`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference"),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        val qualified = RoninConditionProblemsAndHealthConcerns.qualifies(condition)
        assertTrue(qualified)
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference"),
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
            RoninConditionProblemsAndHealthConcerns.validate(condition, null).alertIfErrors()
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
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345")
            ),
            code = null,
            subject = Reference(display = "reference"),
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
            RoninConditionProblemsAndHealthConcerns.validate(condition, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: code is a required element @ Condition.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if not a qualifying category`() {
        val condition = Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345")
            ),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference"),
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
            RoninConditionProblemsAndHealthConcerns.validate(condition, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_CNDPAHC_001: One of the following condition categories required for US Core Condition Problem and Health Concerns profile: problem-list-item, health-concern @ Condition.category",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val condition = Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345")
            ),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference"),
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

        mockkObject(R4ConditionValidator)
        every { R4ConditionValidator.validate(condition, LocationContext(Condition::class)) } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Condition::onset),
                LocationContext(Condition::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            RoninConditionProblemsAndHealthConcerns.validate(condition, null).alertIfErrors()
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
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345")
            ),
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference"),
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

        RoninConditionProblemsAndHealthConcerns.validate(condition, null).alertIfErrors()
    }

    @Test
    fun `transform fails for condition with no ID`() {
        val condition = Condition(
            code = CodeableConcept(text = "code"),
            subject = Reference(display = "reference"),
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

        val transformed = RoninConditionProblemsAndHealthConcerns.transform(condition, tenant)
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
                div = "div"
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
                Identifier(value = "id")
            ),
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("inactive"),
                        display = "Inactive"
                    )
                )
            ),
            verificationStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                        code = Code("confirmed"),
                        display = "Confirmed"
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            ),
            severity = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("371924009"),
                        display = "Moderate to severe"
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer"
                    )
                )
            ),
            bodySite = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://snomed.info/sct"),
                            code = Code("39607008"),
                            display = "Lung structure (body structure)"
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/roninPatientExample01"
            ),
            encounter = Reference(
                reference = "Encounter/roninEncounterExample01"
            ),
            onset = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2019-04-01")),
            abatement = DynamicValue(
                DynamicValueType.PERIOD,
                Period(start = DateTime("2019-04-01"), end = DateTime("2022-04-01"))
            ),
            recordedDate = DateTime("2022-01-01"),
            recorder = Reference(
                reference = "Practitioner/roninPractitionerExample01"
            ),
            asserter = Reference(
                reference = "Practitioner/roninPractitionerExample01"
            ),
            stage = listOf(
                ConditionStage(
                    summary = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://cancerstaging.org"),
                                code = Code("3C"),
                                display = "IIIC"
                            )
                        )
                    )
                )
            ),
            evidence = listOf(
                ConditionEvidence(
                    detail = listOf(
                        Reference(
                            reference = "DiagnosticReport/Test01"
                        )
                    )
                )
            ),
            note = listOf(
                Annotation(
                    author = DynamicValue(
                        DynamicValueType.REFERENCE,
                        Reference(reference = "Practitioner/roninPractitionerExample01")
                    ),
                    text = Markdown("Test")
                )
            )
        )

        val transformed = RoninConditionProblemsAndHealthConcerns.transform(condition, tenant)
        transformed!!
        assertEquals("Condition", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value))),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div"), transformed.text)
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
                Identifier(value = "id"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertEquals(
            CodeableConcept(
                text = "Inactive",
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("inactive"),
                        display = "Inactive"
                    )
                )
            ),
            transformed.clinicalStatus
        )
        assertEquals(
            CodeableConcept(
                text = "Confirmed",
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                        code = Code("confirmed"),
                        display = "Confirmed"
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
                            code = Code("problem-list-item")
                        )
                    )
                )
            ),
            transformed.category
        )
        assertEquals(
            CodeableConcept(
                text = "Moderate to severe",
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("371924009"),
                        display = "Moderate to severe"
                    )
                )
            ),
            transformed.severity
        )
        assertEquals(
            CodeableConcept(
                text = "Non-small cell lung cancer",
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer"
                    )
                )
            ),
            transformed.code
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    text = "Lung structure (body structure)",
                    coding = listOf(
                        Coding(
                            system = Uri("http://snomed.info/sct"),
                            code = Code("39607008"),
                            display = "Lung structure (body structure)"
                        )
                    )
                )
            ),
            transformed.bodySite
        )
        assertEquals(
            Reference(
                reference = "Patient/test-roninPatientExample01"
            ),
            transformed.subject
        )
        assertEquals(
            Reference(
                reference = "Encounter/test-roninEncounterExample01"
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
        assertEquals(Reference(reference = "Practitioner/test-roninPractitionerExample01"), transformed.recorder)
        assertEquals(Reference(reference = "Practitioner/test-roninPractitionerExample01"), transformed.asserter)
        assertEquals(
            listOf(
                ConditionStage(
                    summary = CodeableConcept(
                        text = "IIIC",
                        coding = listOf(
                            Coding(
                                system = Uri("http://cancerstaging.org"),
                                code = Code("3C"),
                                display = "IIIC"
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
                            reference = "DiagnosticReport/test-Test01"
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
                        Reference(reference = "Practitioner/test-roninPractitionerExample01")
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
                Identifier(value = "id")
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer"
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/roninPatientExample01"
            )
        )

        val transformed = RoninConditionProblemsAndHealthConcerns.transform(condition, tenant)
        transformed!!
        assertEquals("Condition", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value))),
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
                Identifier(value = "id"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
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
                            code = Code("problem-list-item")
                        )
                    )
                )
            ),
            transformed.category
        )
        assertNull(transformed.severity)
        assertEquals(
            CodeableConcept(
                text = "Non-small cell lung cancer",
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer"
                    )
                )
            ),
            transformed.code
        )
        assertEquals(listOf<CodeableConcept>(), transformed.bodySite)
        assertEquals(
            Reference(
                reference = "Patient/test-roninPatientExample01"
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
}
