package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.ehr.model.enums.DataSource
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
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.fhir.r4.resource.Condition as R4Condition

class R4ConditionTransformerTest {
    private val transformer = R4ConditionTransformer()
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `non R4 bundle`() {
        val bundle = mockk<Bundle<Condition>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }
        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformConditions(bundle, tenant)
        }
        assertEquals("Bundle is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `non R4 condition`() {
        val condition = mockk<Condition> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformCondition(condition, tenant)
        }

        assertEquals("Condition is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `transforms condition with all attributes`() {
        val r4Condition = R4Condition(
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
            abatement = DynamicValue(DynamicValueType.PERIOD, Period(start = DateTime("2019-04-01"), end = DateTime("2022-04-01"))),
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
                    author = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Practitioner/roninPractitionerExample01")),
                    text = Markdown("Test")
                )
            )
        )
        val condition = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Condition
        }

        val oncologyCondition = transformer.transformCondition(condition, tenant)
        oncologyCondition!!
        assertEquals("Condition", oncologyCondition.resourceType)
        assertEquals(Id("test-12345"), oncologyCondition.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-condition"))),
            oncologyCondition.meta
        )
        assertEquals(Uri("implicit-rules"), oncologyCondition.implicitRules)
        assertEquals(Code("en-US"), oncologyCondition.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), oncologyCondition.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"13579"}""")),
            oncologyCondition.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyCondition.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyCondition.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyCondition.identifier
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
            oncologyCondition.clinicalStatus
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
            oncologyCondition.verificationStatus
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
            oncologyCondition.category
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
            oncologyCondition.severity
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
            oncologyCondition.code
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
            oncologyCondition.bodySite
        )
        assertEquals(
            Reference(
                reference = "Patient/test-roninPatientExample01"
            ),
            oncologyCondition.subject
        )
        assertEquals(
            Reference(
                reference = "Encounter/test-roninEncounterExample01"
            ),
            oncologyCondition.encounter
        )
        assertEquals(DynamicValue(DynamicValueType.DATE_TIME, DateTime("2019-04-01")), oncologyCondition.onset)
        assertEquals(DynamicValue(DynamicValueType.PERIOD, Period(start = DateTime("2019-04-01"), end = DateTime("2022-04-01"))), oncologyCondition.abatement)
        assertEquals(DateTime("2022-01-01"), oncologyCondition.recordedDate)
        assertEquals(Reference(reference = "Practitioner/test-roninPractitionerExample01"), oncologyCondition.recorder)
        assertEquals(Reference(reference = "Practitioner/test-roninPractitionerExample01"), oncologyCondition.asserter)
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
            oncologyCondition.stage
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
            oncologyCondition.evidence
        )
        assertEquals(
            listOf(
                Annotation(
                    author = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Practitioner/test-roninPractitionerExample01")),
                    text = Markdown("Test")
                )
            ),
            oncologyCondition.note
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
            oncologyCondition.code
        )
    }

    @Test
    fun `transforms condition with only required attributes`() {
        val r4Condition = R4Condition(
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
        val condition = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Condition
        }

        val oncologyCondition = transformer.transformCondition(condition, tenant)
        oncologyCondition!!
        assertEquals("Condition", oncologyCondition.resourceType)
        assertEquals(Id("test-12345"), oncologyCondition.id)
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyCondition.identifier
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
            oncologyCondition.category
        )
        assertEquals(
            Reference(
                reference = "Patient/test-roninPatientExample01"
            ),
            oncologyCondition.subject
        )
    }

    @Test
    fun `no id for condition`() {
        val r4Condition = R4Condition(
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
        val condition = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Condition
        }
        assertNull(transformer.transformCondition(condition, tenant))
    }

    @Test
    fun `category cannot be empty`() {
        val r4Condition = R4Condition(
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
        val condition = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Condition
        }
        assertNull(transformer.transformCondition(condition, tenant))
    }

    @Test
    fun `code cannot be null`() {
        val r4Condition = R4Condition(
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
        val condition = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Condition
        }
        assertNull(transformer.transformCondition(condition, tenant))
    }

    @Test
    fun `clinical status must be valid code if populated, condition transformation returns empty when not valid`() {
        val r4Condition = R4Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("potato"),
                        display = "Potato"
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
        val condition = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Condition
        }
        assertNull(transformer.transformCondition(condition, tenant))
    }

    @Test
    fun `bundle transformation returns empty when no valid transformations`() {
        val invalidCondition = R4Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    id = "testId"
                )
            ),
            verificationStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                        code = Code("potato"),
                        display = "Potato"
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
        val condition1 = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidCondition
        }
        val condition2 = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidCondition
        }

        val bundle = mockk<Bundle<Condition>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(condition1, condition2)
        }

        val oncologyConditions = transformer.transformConditions(bundle, tenant)
        assertEquals(0, oncologyConditions.size)
    }

    @Test
    fun `bundle transformation returns only valid transformations`() {
        val invalidCondition = R4Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    id = "testId"
                )
            ),
            verificationStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                        code = Code("potato"),
                        display = "Potato"
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
        val r4Condition = R4Condition(
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
        val condition1 = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidCondition
        }
        val condition2 = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Condition
        }

        val bundle = mockk<Bundle<Condition>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(condition1, condition2)
        }

        val oncologyConditions = transformer.transformConditions(bundle, tenant)
        assertEquals(1, oncologyConditions.size)
    }

    @Test
    fun `bundle transformation returns all when all valid`() {
        val r4Condition = R4Condition(
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
        val condition1 = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Condition
        }
        val condition2 = mockk<Condition> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Condition
        }

        val bundle = mockk<Bundle<Condition>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(condition1, condition2)
        }

        val oncologyConditions = transformer.transformConditions(bundle, tenant)
        assertEquals(2, oncologyConditions.size)
    }
}
