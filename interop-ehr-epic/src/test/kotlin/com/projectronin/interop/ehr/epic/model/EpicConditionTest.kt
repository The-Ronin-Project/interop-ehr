package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.exception.UnsupportedDynamicValueTypeException
import com.projectronin.interop.ehr.model.Condition.AgeAbatement
import com.projectronin.interop.ehr.model.Condition.AgeOnset
import com.projectronin.interop.ehr.model.Condition.DateTimeAbatement
import com.projectronin.interop.ehr.model.Condition.DateTimeOnset
import com.projectronin.interop.ehr.model.Condition.PeriodAbatement
import com.projectronin.interop.ehr.model.Condition.PeriodOnset
import com.projectronin.interop.ehr.model.Condition.RangeAbatement
import com.projectronin.interop.ehr.model.Condition.RangeOnset
import com.projectronin.interop.ehr.model.Condition.StringAbatement
import com.projectronin.interop.ehr.model.Condition.StringOnset
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Age
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ConditionEvidence
import com.projectronin.interop.fhir.r4.datatype.ConditionStage
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Range
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.SimpleQuantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Condition
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicConditionTest {
    @Test
    fun `can build from object`() {
        val identifier = Identifier(system = Uri("abc123"), value = "E14345")
        val clinicalStatus = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                    version = "4.0.0",
                    code = Code("resolved"),
                    display = "Resolved"
                )
            ),
            text = "Resolved"
        )
        val verificationStatus = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                    version = "4.0.0",
                    code = Code("confirmed"),
                    display = "Confirmed"
                )
            ),
            text = "Confirmed"
        )
        val category = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-category"),
                    code = Code("problem-list-item"),
                    display = "Problem List Item"
                )
            ),
            text = "Problem List Item"
        )
        val severity = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("24484000"),
                    display = "Severe"
                )
            )
        )
        val code = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("39065001"),
                )
            ),
            text = "Burn of ear"
        )
        val bodySite = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("49521004"),
                    display = "Left external ear structure"
                )
            ),
            text = "Left Ear"
        )
        val subject = Reference(display = "1234")
        val encounter = Reference(display = "5678")
        val onset = DynamicValue(DynamicValueType.STRING, "Onset")
        val abatement = DynamicValue(DynamicValueType.STRING, "Abatement")
        val recorder = Reference(display = "9012")
        val asserter = Reference(display = "13579")
        val stage = ConditionStage(
            assessment = listOf(
                Reference(display = "Genetic analysis master panel")
            )
        )
        val evidence = ConditionEvidence(
            detail = listOf(
                Reference(display = "Temperature")
            )
        )
        val note = Annotation(text = Markdown("The patient is anuric."))

        val condition = Condition(
            id = Id("eGVC1DSR9YDJxMi7Th3xbsA3"),
            identifier = listOf(identifier),
            clinicalStatus = clinicalStatus,
            verificationStatus = verificationStatus,
            category = listOf(category),
            severity = severity,
            code = code,
            bodySite = listOf(bodySite),
            subject = subject,
            encounter = encounter,
            onset = onset,
            abatement = abatement,
            recordedDate = DateTime("2022-03-01"),
            recorder = recorder,
            asserter = asserter,
            stage = listOf(stage),
            evidence = listOf(evidence),
            note = listOf(note)
        )

        val epicCondition = EpicCondition(condition)
        assertEquals(condition, epicCondition.resource)
        assertEquals(DataSource.FHIR_R4, epicCondition.dataSource)
        assertEquals(ResourceType.CONDITION, epicCondition.resourceType)
        assertEquals("eGVC1DSR9YDJxMi7Th3xbsA3", epicCondition.id)

        assertEquals(1, epicCondition.identifier.size)
        assertEquals(identifier, epicCondition.identifier[0].element)

        assertEquals(clinicalStatus, epicCondition.clinicalStatus!!.element)
        assertEquals(verificationStatus, epicCondition.verificationStatus!!.element)

        assertEquals(1, epicCondition.category.size)
        assertEquals(category, epicCondition.category[0].element)

        assertEquals(severity, epicCondition.severity!!.element)
        assertEquals(code, epicCondition.code!!.element)

        assertEquals(1, epicCondition.bodySite.size)
        assertEquals(bodySite, epicCondition.bodySite[0].element)

        assertEquals(subject, epicCondition.subject.element)
        assertEquals(encounter, epicCondition.encounter!!.element)

        // Other tests are verifying the details of onset and abatement, so we're just going to verify they're not null
        assertNotNull(epicCondition.onset)
        assertNotNull(epicCondition.abatement)

        assertEquals("2022-03-01", epicCondition.recordedDate)
        assertEquals(recorder, epicCondition.recorder!!.element)
        assertEquals(asserter, epicCondition.asserter!!.element)

        assertEquals(1, epicCondition.stage.size)
        assertEquals(stage, epicCondition.stage[0].element)

        assertEquals(1, epicCondition.evidence.size)
        assertEquals(evidence, epicCondition.evidence[0].element)

        assertEquals(1, epicCondition.note.size)
        assertEquals(note, epicCondition.note[0].element)
    }

    @Test
    fun `can build from null and missing values`() {
        val condition = Condition(
            id = Id("eGVC1DSR9YDJxMi7Th3xbsA3"),
            identifier = listOf(),
            category = listOf(),
            code = null,
            subject = Reference(display = "1234")
        )

        val epicCondition = EpicCondition(condition)
        assertEquals(condition, epicCondition.resource)
        assertEquals(DataSource.FHIR_R4, epicCondition.dataSource)
        assertEquals(ResourceType.CONDITION, epicCondition.resourceType)
        assertEquals("eGVC1DSR9YDJxMi7Th3xbsA3", epicCondition.id)
        assertEquals(0, epicCondition.identifier.size)
        assertNull(epicCondition.clinicalStatus)
        assertNull(epicCondition.verificationStatus)
        assertEquals(0, epicCondition.category.size)
        assertNull(epicCondition.severity)
        assertNull(epicCondition.code)
        assertEquals(0, epicCondition.bodySite.size)
        assertEquals(Reference(display = "1234"), epicCondition.subject.element)
        assertNull(epicCondition.encounter)
        assertNull(epicCondition.onset)
        assertNull(epicCondition.abatement)
        assertNull(epicCondition.recordedDate)
        assertNull(epicCondition.recorder)
        assertNull(epicCondition.asserter)
        assertEquals(0, epicCondition.stage.size)
        assertEquals(0, epicCondition.evidence.size)
        assertEquals(0, epicCondition.note.size)
    }

    @Test
    fun `return JSON from raw`() {
        val identifier = Identifier(system = Uri("abc123"), value = "E14345")
        val clinicalStatus = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                    version = "4.0.0",
                    code = Code("resolved"),
                    display = "Resolved"
                )
            ),
            text = "Resolved"
        )
        val verificationStatus = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                    version = "4.0.0",
                    code = Code("confirmed"),
                    display = "Confirmed"
                )
            ),
            text = "Confirmed"
        )
        val category = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-category"),
                    code = Code("problem-list-item"),
                    display = "Problem List Item"
                )
            ),
            text = "Problem List Item"
        )
        val severity = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("24484000"),
                    display = "Severe"
                )
            )
        )
        val code = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("39065001"),
                )
            ),
            text = "Burn of ear"
        )
        val bodySite = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("49521004"),
                    display = "Left external ear structure"
                )
            ),
            text = "Left Ear"
        )
        val subject = Reference(display = "1234")
        val encounter = Reference(display = "5678")
        val onset = DynamicValue(DynamicValueType.STRING, "Onset")
        val abatement = DynamicValue(DynamicValueType.STRING, "Abatement")
        val recorder = Reference(display = "9012")
        val asserter = Reference(display = "13579")
        val stage = ConditionStage(
            assessment = listOf(
                Reference(display = "Genetic analysis master panel")
            )
        )
        val evidence = ConditionEvidence(
            detail = listOf(
                Reference(display = "Temperature")
            )
        )
        val note = Annotation(text = Markdown("The patient is anuric."))

        val condition = Condition(
            id = Id("eGVC1DSR9YDJxMi7Th3xbsA3"),
            identifier = listOf(identifier),
            clinicalStatus = clinicalStatus,
            verificationStatus = verificationStatus,
            category = listOf(category),
            severity = severity,
            code = code,
            bodySite = listOf(bodySite),
            subject = subject,
            encounter = encounter,
            onset = onset,
            abatement = abatement,
            recordedDate = DateTime("2022-03-01"),
            recorder = recorder,
            asserter = asserter,
            stage = listOf(stage),
            evidence = listOf(evidence),
            note = listOf(note)
        )

        val identifierJSON = """{"system":"abc123","value":"E14345"}"""
        val clinicalStatusJSON =
            """{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/condition-clinical","version":"4.0.0","code":"resolved","display":"Resolved"}],"text":"Resolved"}"""
        val verificationStatusJSON =
            """{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/condition-ver-status","version":"4.0.0","code":"confirmed","display":"Confirmed"}],"text":"Confirmed"}"""
        val categoryJSON =
            """{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/condition-category","code":"problem-list-item","display":"Problem List Item"}],"text":"Problem List Item"}"""
        val severityJSON = """{"coding":[{"system":"http://snomed.info/sct","code":"24484000","display":"Severe"}]}"""
        val codeJSON = """{"coding":[{"system":"http://snomed.info/sct","code":"39065001"}],"text":"Burn of ear"}"""
        val bodySiteJSON =
            """{"coding":[{"system":"http://snomed.info/sct","code":"49521004","display":"Left external ear structure"}],"text":"Left Ear"}"""
        val subjectJSON = """{"display":"1234"}"""
        val encounterJSON = """{"display":"5678"}"""
        val recorderJSON = """{"display":"9012"}"""
        val asserterJSON = """{"display":"13579"}"""
        val stageJSON = """{"assessment":[{"display":"Genetic analysis master panel"}]}"""
        val evidenceJSON = """{"detail":[{"display":"Temperature"}]}"""
        val noteJSON = """{"text":"The patient is anuric."}"""

        val json = """
            {
                "resourceType": "Condition",
                "id": "eGVC1DSR9YDJxMi7Th3xbsA3",
                "identifier": [$identifierJSON],
                "clinicalStatus": $clinicalStatusJSON,
                "verificationStatus": $verificationStatusJSON,
                "category": [$categoryJSON],
                "severity": $severityJSON,
                "code": $codeJSON,
                "bodySite": [$bodySiteJSON],
                "subject": $subjectJSON,
                "encounter": $encounterJSON,
                "onsetString": "Onset",
                "abatementString": "Abatement",
                "recordedDate": "2022-03-01",
                "recorder": $recorderJSON,
                "asserter": $asserterJSON,
                "stage": [$stageJSON],
                "evidence": [$evidenceJSON],
                "note": [$noteJSON]
              }
        """.trimIndent()

        val epicCondition = EpicCondition(condition)
        assertEquals(condition, epicCondition.resource)
        assertEquals(deformat(json), epicCondition.raw)

        assertEquals(1, epicCondition.identifier.size)
        assertEquals(identifier, epicCondition.identifier[0].element)
        assertEquals(identifierJSON, epicCondition.identifier[0].raw)

        assertEquals(clinicalStatus, epicCondition.clinicalStatus!!.element)
        assertEquals(clinicalStatusJSON, epicCondition.clinicalStatus!!.raw)

        assertEquals(verificationStatus, epicCondition.verificationStatus!!.element)
        assertEquals(verificationStatusJSON, epicCondition.verificationStatus!!.raw)

        assertEquals(1, epicCondition.category.size)
        assertEquals(category, epicCondition.category[0].element)
        assertEquals(categoryJSON, epicCondition.category[0].raw)

        assertEquals(severity, epicCondition.severity!!.element)
        assertEquals(severityJSON, epicCondition.severity!!.raw)

        assertEquals(code, epicCondition.code!!.element)
        assertEquals(codeJSON, epicCondition.code!!.raw)

        assertEquals(1, epicCondition.bodySite.size)
        assertEquals(bodySite, epicCondition.bodySite[0].element)
        assertEquals(bodySiteJSON, epicCondition.bodySite[0].raw)

        assertEquals(subject, epicCondition.subject.element)
        assertEquals(subjectJSON, epicCondition.subject.raw)

        assertEquals(encounter, epicCondition.encounter!!.element)
        assertEquals(encounterJSON, epicCondition.encounter!!.raw)

        assertEquals(recorder, epicCondition.recorder!!.element)
        assertEquals(recorderJSON, epicCondition.recorder!!.raw)

        assertEquals(asserter, epicCondition.asserter!!.element)
        assertEquals(asserterJSON, epicCondition.asserter!!.raw)

        assertEquals(1, epicCondition.stage.size)
        assertEquals(stage, epicCondition.stage[0].element)
        assertEquals(stageJSON, epicCondition.stage[0].raw)

        assertEquals(1, epicCondition.evidence.size)
        assertEquals(evidence, epicCondition.evidence[0].element)
        assertEquals(evidenceJSON, epicCondition.evidence[0].raw)

        assertEquals(1, epicCondition.note.size)
        assertEquals(note, epicCondition.note[0].element)
        assertEquals(noteJSON, epicCondition.note[0].raw)
    }

    @Test
    fun `handles onsetDateTime`() {
        val condition = Condition(
            id = Id("123"),
            subject = Reference(display = "1234"),
            onset = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2021-06-01"))
        )
        val epicCondition = EpicCondition(condition)

        val onset = epicCondition.onset!! as DateTimeOnset
        assertEquals("2021-06-01", onset.value)
    }

    @Test
    fun `handles onsetAge`() {
        val age = Age(value = 20.0, code = Code("y"), system = CodeSystem.UCUM.uri)
        val condition = Condition(
            id = Id("123"),
            subject = Reference(display = "1234"),
            onset = DynamicValue(DynamicValueType.AGE, age)
        )
        val epicCondition = EpicCondition(condition)

        val onset = epicCondition.onset!! as AgeOnset
        assertEquals(age, onset.value.element)
    }

    @Test
    fun `handles onsetPeriod`() {
        val period = Period(start = DateTime("2020-03-20"), end = DateTime("2022-05-19"))
        val condition = Condition(
            id = Id("123"),
            subject = Reference(display = "1234"),
            onset = DynamicValue(DynamicValueType.PERIOD, period)
        )
        val epicCondition = EpicCondition(condition)

        val onset = epicCondition.onset!! as PeriodOnset
        assertEquals(period, onset.value.element)
    }

    @Test
    fun `handles onsetRange`() {
        val range = Range(low = SimpleQuantity(value = 5.0), high = SimpleQuantity(value = 7.5))
        val condition = Condition(
            id = Id("123"),
            subject = Reference(display = "1234"),
            onset = DynamicValue(DynamicValueType.RANGE, range)
        )
        val epicCondition = EpicCondition(condition)

        val onset = epicCondition.onset!! as RangeOnset
        assertEquals(range, onset.value.element)
    }

    @Test
    fun `handles onsetString`() {
        val condition = Condition(
            id = Id("123"),
            subject = Reference(display = "1234"),
            onset = DynamicValue(DynamicValueType.STRING, "yesterday")
        )
        val epicCondition = EpicCondition(condition)

        val onset = epicCondition.onset!! as StringOnset
        assertEquals("yesterday", onset.value)
    }

    @Test
    fun `throws exception for unknown onset type`() {
        // We currently prohibit creating a Condition with an invalid type
        val condition = mockk<Condition>(relaxed = true) {
            every { id } returns Id("1234")
            every { onset } returns DynamicValue(DynamicValueType.REFERENCE, Reference(display = "onset"))
        }
        val epicCondition = EpicCondition(condition)

        val exception = assertThrows<UnsupportedDynamicValueTypeException> { epicCondition.onset }
        assertEquals("REFERENCE is not a supported type for condition onset", exception.message)
    }

    @Test
    fun `handles abatementDateTime`() {
        val condition = Condition(
            id = Id("123"),
            subject = Reference(display = "1234"),
            abatement = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2021-06-01")),
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("remission"),
                        display = "Remission"
                    )
                )
            ),
        )
        val epicCondition = EpicCondition(condition)

        val abatement = epicCondition.abatement!! as DateTimeAbatement
        assertEquals("2021-06-01", abatement.value)
    }

    @Test
    fun `handles abatementAge`() {
        val age = Age(value = 20.0, code = Code("y"), system = CodeSystem.UCUM.uri)
        val condition = Condition(
            id = Id("123"),
            subject = Reference(display = "1234"),
            abatement = DynamicValue(DynamicValueType.AGE, age),
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("resolved"),
                        display = "Resolved"
                    )
                )
            ),
        )
        val epicCondition = EpicCondition(condition)

        val abatement = epicCondition.abatement!! as AgeAbatement
        assertEquals(age, abatement.value.element)
    }

    @Test
    fun `handles abatementPeriod`() {
        val period = Period(start = DateTime("2020-03-20"), end = DateTime("2022-05-19"))
        val condition = Condition(
            id = Id("123"),
            subject = Reference(display = "1234"),
            abatement = DynamicValue(DynamicValueType.PERIOD, period),
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("inactive"),
                        display = "Inactive"
                    )
                )
            ),
        )
        val epicCondition = EpicCondition(condition)

        val abatement = epicCondition.abatement!! as PeriodAbatement
        assertEquals(period, abatement.value.element)
    }

    @Test
    fun `handles abatementRange`() {
        val range = Range(low = SimpleQuantity(value = 5.0), high = SimpleQuantity(value = 7.5))
        val condition = Condition(
            id = Id("123"),
            subject = Reference(display = "1234"),
            abatement = DynamicValue(DynamicValueType.RANGE, range),
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("inactive"),
                        display = "Inactive"
                    )
                )
            ),
        )
        val epicCondition = EpicCondition(condition)

        val abatement = epicCondition.abatement!! as RangeAbatement
        assertEquals(range, abatement.value.element)
    }

    @Test
    fun `handles abatementString`() {
        val condition = Condition(
            id = Id("123"),
            subject = Reference(display = "1234"),
            abatement = DynamicValue(DynamicValueType.STRING, "yesterday"),
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("resolved"),
                        display = "Resolved"
                    )
                )
            ),
        )
        val epicCondition = EpicCondition(condition)

        val abatement = epicCondition.abatement!! as StringAbatement
        assertEquals("yesterday", abatement.value)
    }

    @Test
    fun `throws exception for unknown abatement type`() {
        // We currently prohibit creating a Condition with an invalid type
        val condition = mockk<Condition>(relaxed = true) {
            every { id } returns Id("1234")
            every { abatement } returns DynamicValue(DynamicValueType.REFERENCE, Reference(display = "abatement"))
        }
        val epicCondition = EpicCondition(condition)

        val exception = assertThrows<UnsupportedDynamicValueTypeException> { epicCondition.abatement }
        assertEquals("REFERENCE is not a supported type for condition abatement", exception.message)
    }

    @Test
    fun `can build stage from object`() {
        val summary = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("14803004"),
                    display = "Temporary"
                )
            )
        )
        val assessment = Reference(display = "Genetic analysis master panel")
        val type = CodeableConcept(text = "Type")
        val stage = ConditionStage(
            summary = summary,
            assessment = listOf(assessment),
            type = type
        )

        val epicStage = EpicCondition.EpicStage(stage)
        assertEquals(stage, epicStage.element)
        assertEquals(summary, epicStage.summary?.element)

        assertEquals(1, epicStage.assessment.size)
        assertEquals(assessment, epicStage.assessment[0].element)

        assertEquals(type, epicStage.type?.element)
    }

    @Test
    fun `can build stage from null and missing values`() {
        val assessment = Reference(display = "Genetic analysis master panel")
        val stage = ConditionStage(
            summary = null,
            assessment = listOf(assessment)
        )

        val epicStage = EpicCondition.EpicStage(stage)
        assertEquals(stage, epicStage.element)
        assertNull(epicStage.summary?.element)

        assertEquals(1, epicStage.assessment.size)
        assertEquals(assessment, epicStage.assessment[0].element)

        assertNull(epicStage.type?.element)
    }

    @Test
    fun `returns JSON from raw stage`() {
        val summary = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("14803004"),
                    display = "Temporary"
                )
            )
        )
        val assessment = Reference(display = "Genetic analysis master panel")
        val type = CodeableConcept(text = "Type")
        val stage = ConditionStage(
            summary = summary,
            assessment = listOf(assessment),
            type = type
        )

        val summaryJSON = """{"coding":[{"system":"http://snomed.info/sct","code":"14803004","display":"Temporary"}]}"""
        val assessmentJSON = """{"display":"Genetic analysis master panel"}"""
        val typeJSON = """{"text":"Type"}"""

        val json = """
            {
              "summary": $summaryJSON,
              "assessment": [$assessmentJSON],
              "type": $typeJSON
            }
        """.trimIndent()

        val epicStage = EpicCondition.EpicStage(stage)
        assertEquals(stage, epicStage.element)
        assertEquals(deformat(json), epicStage.raw)

        assertEquals(summary, epicStage.summary?.element)
        assertEquals(summaryJSON, epicStage.summary?.raw)

        assertEquals(1, epicStage.assessment.size)
        assertEquals(assessment, epicStage.assessment[0].element)
        assertEquals(assessmentJSON, epicStage.assessment[0].raw)

        assertEquals(type, epicStage.type?.element)
        assertEquals(typeJSON, epicStage.type?.raw)
    }

    @Test
    fun `can build evidence from object`() {
        val code = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("258710007"),
                    display = "degrees C"
                )
            )
        )
        val detail = Reference(display = "Temperature")
        val evidence = ConditionEvidence(
            code = listOf(code),
            detail = listOf(detail)
        )

        val epicEvidence = EpicCondition.EpicEvidence(evidence)
        assertEquals(evidence, epicEvidence.element)

        assertEquals(1, epicEvidence.code.size)
        assertEquals(code, epicEvidence.code[0].element)

        assertEquals(1, epicEvidence.detail.size)
        assertEquals(detail, epicEvidence.detail[0].element)
    }

    @Test
    fun `can build evidence from null and missing values`() {
        val detail = Reference(display = "Temperature")
        val evidence = ConditionEvidence(
            detail = listOf(detail)
        )

        val epicEvidence = EpicCondition.EpicEvidence(evidence)
        assertEquals(evidence, epicEvidence.element)

        assertEquals(0, epicEvidence.code.size)

        assertEquals(1, epicEvidence.detail.size)
        assertEquals(detail, epicEvidence.detail[0].element)
    }

    @Test
    fun `returns JSON from raw evidence`() {
        val code = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("258710007"),
                    display = "degrees C"
                )
            )
        )
        val detail = Reference(display = "Temperature")
        val evidence = ConditionEvidence(
            code = listOf(code),
            detail = listOf(detail)
        )

        val codeJSON = """{"coding":[{"system":"http://snomed.info/sct","code":"258710007","display":"degrees C"}]}"""
        val detailJSON = """{"display":"Temperature"}"""
        val json = """{"code":[$codeJSON],"detail":[$detailJSON]}"""

        val epicEvidence = EpicCondition.EpicEvidence(evidence)
        assertEquals(evidence, epicEvidence.element)
        assertEquals(json, epicEvidence.raw)

        assertEquals(1, epicEvidence.code.size)
        assertEquals(code, epicEvidence.code[0].element)
        assertEquals(codeJSON, epicEvidence.code[0].raw)

        assertEquals(1, epicEvidence.detail.size)
        assertEquals(detail, epicEvidence.detail[0].element)
        assertEquals(detailJSON, epicEvidence.detail[0].raw)
    }
}
