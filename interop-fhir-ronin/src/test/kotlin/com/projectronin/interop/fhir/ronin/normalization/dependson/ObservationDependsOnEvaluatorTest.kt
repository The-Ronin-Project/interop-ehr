package com.projectronin.interop.fhir.ronin.normalization.dependson

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ConceptMapDependsOn
import com.projectronin.interop.fhir.r4.resource.Observation
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObservationDependsOnEvaluatorTest {
    private val evaluator = ObservationDependsOnEvaluator()

    @Test
    fun `reports as Observation`() {
        assertEquals(Observation::class, evaluator.resourceType)
    }

    @Test
    fun `unimplemented property returns as not met`() {
        val dependsOn = ConceptMapDependsOn(
            property = Uri("observation.unknown.field"),
            value = FHIRString("value")
        )
        val observation = mockk<Observation>()
        val met = evaluator.meetsDependsOn(observation, listOf(dependsOn))
        assertFalse(met)
    }

    @Test
    fun `depends on code text when resource code text is null`() {
        val dependsOn = ConceptMapDependsOn(
            property = Uri("observation.code.text"),
            value = FHIRString("code text")
        )
        val observation = mockk<Observation> {
            every { code?.text } returns null
        }
        val met = evaluator.meetsDependsOn(observation, listOf(dependsOn))
        assertFalse(met)
    }

    @Test
    fun `depends on code text that matches resource code text`() {
        val dependsOn = ConceptMapDependsOn(
            property = Uri("observation.code.text"),
            value = FHIRString("code text")
        )
        val observation = mockk<Observation> {
            every { code?.text?.value } returns "code text"
        }
        val met = evaluator.meetsDependsOn(observation, listOf(dependsOn))
        assertTrue(met)
    }

    @Test
    fun `depends on code text that does not match resource code text`() {
        val dependsOn = ConceptMapDependsOn(
            property = Uri("observation.code.text"),
            value = FHIRString("code text")
        )
        val observation = mockk<Observation> {
            every { code?.text?.value } returns "this is my code text"
        }
        val met = evaluator.meetsDependsOn(observation, listOf(dependsOn))
        assertFalse(met)
    }

    @Test
    fun `depends on code when resource code is null`() {
        val dependsOn = ConceptMapDependsOn(
            property = Uri("observation.code"),
            value = FHIRString("{\"coding\":[{\"code\":\"SNOMED#399651003\",\"system\":\"http://snomed.info/sct\"},{\"code\":\"EPIC#46451\",\"display\":\"stage date\",\"system\":\"urn:oid:1.2.840.114350.1.13.412.2.7.2.727688\"}],\"text\":\"FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - STAGE DATE\"}")
        )
        val observation = mockk<Observation> {
            every { code } returns null
        }
        val met = evaluator.meetsDependsOn(observation, listOf(dependsOn))
        assertFalse(met)
    }

    @Test
    fun `depends on code that matches resource code`() {
        val dependsOn = ConceptMapDependsOn(
            property = Uri("observation.code"),
            value = FHIRString("{\"coding\":[{\"code\":\"SNOMED#399651003\",\"system\":\"http://snomed.info/sct\"},{\"code\":\"EPIC#46451\",\"display\":\"stage date\",\"system\":\"urn:oid:1.2.840.114350.1.13.412.2.7.2.727688\"}],\"text\":\"FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - STAGE DATE\"}")
        )
        val observation = mockk<Observation> {
            every { code } returns
                CodeableConcept(
                    coding = listOf(
                        Coding(code = Code("SNOMED#399651003"), system = Uri("http://snomed.info/sct")),
                        Coding(code = Code("EPIC#46451"), system = Uri("urn:oid:1.2.840.114350.1.13.412.2.7.2.727688"), display = FHIRString("stage date"))
                    ),
                    text = FHIRString("FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - STAGE DATE")
                )
        }
        val met = evaluator.meetsDependsOn(observation, listOf(dependsOn))
        assertTrue(met)
    }

    @Test
    fun `depends on code that does not match resource code`() {
        val dependsOn = ConceptMapDependsOn(
            property = Uri("observation.code"),
            value = FHIRString("{\"coding\":[{\"code\":\"SNOMED#399651003\",\"system\":\"http://snomed.info/sct\"},{\"code\":\"EPIC#46451\",\"display\":\"stage date\",\"system\":\"urn:oid:1.2.840.114350.1.13.412.2.7.2.727688\"}],\"text\":\"FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - STAGE DATE\"}")
        )
        val observation = mockk<Observation> {
            every { code } returns
                CodeableConcept(
                    coding = listOf(
                        Coding(code = Code("FAKE#345203234"), system = Uri("http://snomed.info/sct")),
                        Coding(code = Code("WRONG#234237"), system = Uri("urn:oid:1.2.840.114350.1.13.412.2.7.2.727688"), display = FHIRString("stage date"))
                    ),
                    text = FHIRString("FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - STAGE DATE")
                )
        }
        val met = evaluator.meetsDependsOn(observation, listOf(dependsOn))
        assertFalse(met)
    }

    @Test
    fun `depends on code with invalid value type fails`() {
        val dependsOn = ConceptMapDependsOn(
            property = Uri("observation.code"),
            value = FHIRString("INVALID_JSON")
        )
        val observation = mockk<Observation> {
            every { code } returns
                CodeableConcept(
                    coding = listOf(
                        Coding(code = Code("SNOMED#399651003"), system = Uri("http://snomed.info/sct")),
                        Coding(code = Code("EPIC#46451"), system = Uri("urn:oid:1.2.840.114350.1.13.412.2.7.2.727688"), display = FHIRString("stage date"))
                    ),
                    text = FHIRString("FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - STAGE DATE")
                )
        }
        val met = evaluator.meetsDependsOn(observation, listOf(dependsOn))
        assertFalse(met)
    }
}
