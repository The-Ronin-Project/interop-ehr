package com.projectronin.interop.fhir.ronin.normalization.dependson

import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ConceptMapDependsOn
import com.projectronin.interop.fhir.r4.resource.Observation
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BaseDependsOnEvaluatorTest {
    @Test
    fun `throws exception if no property is set on dependsOn`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn = ConceptMapDependsOn(
            property = null,
            value = FHIRString("value")
        )
        val exception = assertThrows<IllegalStateException> { evaluator.meetsDependsOn(mockk(), listOf(dependsOn)) }
        assertEquals("Null property found for DependsOn: $dependsOn", exception.message)
    }

    @Test
    fun `throws exception if no value is set on dependsOn`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn = ConceptMapDependsOn(
            property = Uri("observation.code"),
            value = null
        )
        val exception = assertThrows<IllegalStateException> { evaluator.meetsDependsOn(mockk(), listOf(dependsOn)) }
        assertEquals("Null value found for DependsOn: $dependsOn", exception.message)
    }

    @Test
    fun `treats properties as case-sensitive`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn = ConceptMapDependsOn(
            property = Uri("Observation.cOdE"),
            value = FHIRString("value")
        )
        val met = evaluator.meetsDependsOn(mockk(), listOf(dependsOn))
        assertTrue(met)

        assertEquals(mapOf("observation.code" to listOf("value")), evaluator.propertyToValue)
    }

    @Test
    fun `returns true when all dependsOn are met`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn1 = ConceptMapDependsOn(
            property = Uri("Observation.cOdE"),
            value = FHIRString("value")
        )
        val dependsOn2 = ConceptMapDependsOn(
            property = Uri("observation.code"),
            value = FHIRString("true")
        )
        val met = evaluator.meetsDependsOn(mockk(), listOf(dependsOn1, dependsOn2))
        assertTrue(met)

        assertEquals(mapOf("observation.code" to listOf("value", "true")), evaluator.propertyToValue)
    }

    @Test
    fun `returns false when single dependsOn is not met`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn1 = ConceptMapDependsOn(
            property = Uri("Observation.cOdE"),
            value = FHIRString("value")
        )
        val dependsOn2 = ConceptMapDependsOn(
            property = Uri("observation.code"),
            value = FHIRString("false")
        )
        val met = evaluator.meetsDependsOn(mockk(), listOf(dependsOn1, dependsOn2))
        assertFalse(met)

        assertEquals(mapOf("observation.code" to listOf("value", "false")), evaluator.propertyToValue)
    }

    class CapturingDependsOnEvaluator : BaseDependsOnEvaluator<Observation>(Observation::class) {
        val propertyToValue = mutableMapOf<String, MutableList<String>>()

        override fun meetsDependsOn(
            resource: Observation,
            normalizedProperty: String,
            dependsOnValue: String
        ): Boolean {
            propertyToValue.computeIfAbsent(normalizedProperty) { mutableListOf() }.add(dependsOnValue)
            return dependsOnValue != "false"
        }
    }
}
