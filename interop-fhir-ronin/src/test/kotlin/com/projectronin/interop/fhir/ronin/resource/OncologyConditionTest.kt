package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
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
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OncologyConditionTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `validate fails if no tenant identifier provided`() {
        val condition = Condition(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.MRN.uri,
                    type = CodeableConcepts.MRN,
                    value = "MRN"
                ),
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            code = Code("encounter-diagnosis")
                        )
                    ),
                    text = "Encounter Diagnosis"
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
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyCondition.validate(condition).alertIfErrors()
            }
        assertEquals("Tenant identifier is required", exception.message)
    }

    @Test
    fun `validate category cannot be empty`() {
        val condition = Condition(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            category = listOf(),
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
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyCondition.validate(condition).alertIfErrors()
            }
        assertEquals("At least one category must be provided", exception.message)
    }

    @Test
    fun `validate fails for multiple issues`() {
        val condition = Condition(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.MRN.uri,
                    type = CodeableConcepts.MRN,
                    value = "MRN"
                ),
            ),
            category = listOf(),
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
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyCondition.validate(condition).alertIfErrors()
            }
        assertEquals(
            "Encountered multiple validation errors:\nTenant identifier is required\nAt least one category must be provided",
            exception.message
        )
    }

    @Test
    fun `validate succeeds for valid condition`() {
        val condition = Condition(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            code = Code("encounter-diagnosis")
                        )
                    ),
                    text = "Encounter Diagnosis"
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

        OncologyCondition.validate(condition)
    }

    @Test
    fun `transforms condition with all attributes`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-condition"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED,
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
                            code = Code("encounter-diagnosis")
                        )
                    ),
                    text = "Encounter Diagnosis"
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

        val transformed = OncologyCondition.transform(condition, tenant)
        transformed!!
        assertEquals("Condition", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-condition"))),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), transformed.text)
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
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertEquals(
            CodeableConcept(
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
                            code = Code("encounter-diagnosis")
                        )
                    ),
                    text = "Encounter Diagnosis"
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
                        display = "Moderate to severe"
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
                        display = "Non-small cell lung cancer"
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
        assertEquals(
            CodeableConcept(
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
                            code = Code("encounter-diagnosis")
                        )
                    ),
                    text = "Encounter Diagnosis"
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

        val transformed = OncologyCondition.transform(condition, tenant)
        transformed!!
        assertEquals("Condition", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            code = Code("encounter-diagnosis")
                        )
                    ),
                    text = "Encounter Diagnosis"
                )
            ),
            transformed.category
        )
        assertEquals(
            Reference(
                reference = "Patient/test-roninPatientExample01"
            ),
            transformed.subject
        )
    }

    @Test
    fun `transform fails for no id`() {
        val condition = Condition(
            identifier = listOf(
                Identifier(value = "id")
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            code = Code("encounter-diagnosis")
                        )
                    ),
                    text = "Encounter Diagnosis"
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

        val transformed = OncologyCondition.transform(condition, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform fails for empty category`() {
        val condition = Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            category = listOf(),
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

        val transformed = OncologyCondition.transform(condition, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform fails for null code`() {
        val condition = Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(value = "id")
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            code = Code("encounter-diagnosis")
                        )
                    ),
                    text = "Encounter Diagnosis"
                )
            ),
            subject = Reference(
                reference = "Patient/roninPatientExample01"
            )
        )

        val transformed = OncologyCondition.transform(condition, tenant)
        assertNull(transformed)
    }
}
