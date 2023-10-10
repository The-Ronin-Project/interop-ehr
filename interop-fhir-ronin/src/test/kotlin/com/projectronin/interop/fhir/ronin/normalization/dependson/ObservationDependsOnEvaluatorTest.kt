package com.projectronin.interop.fhir.ronin.normalization.dependson

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
}
