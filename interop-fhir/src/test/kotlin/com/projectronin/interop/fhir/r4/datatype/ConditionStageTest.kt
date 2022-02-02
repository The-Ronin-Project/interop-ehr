package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConditionStageTest {
    @Test
    fun `fails if value provided without summary or assessment`() {
        val exception =
            assertThrows<IllegalArgumentException> { ConditionStage(id = "id") }
        assertEquals("Stage SHALL have summary or assessment", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val conditionStage = ConditionStage(
            id = "12345",
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
            summary = CodeableConcept(text = "summary"),
            assessment = listOf(Reference(display = "assessment")),
            type = CodeableConcept(text = "type")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(conditionStage)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "modifierExtension" : [ {
            |    "url" : "http://localhost/modifier-extension",
            |    "valueString" : "Value"
            |  } ],
            |  "summary" : {
            |    "text" : "summary"
            |  },
            |  "assessment" : [ {
            |    "display" : "assessment"
            |  } ],
            |  "type" : {
            |    "text" : "type"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedConditionStage = objectMapper.readValue<ConditionStage>(json)
        assertEquals(conditionStage, deserializedConditionStage)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val conditionStage = ConditionStage(
            assessment = listOf(Reference(display = "assessment"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(conditionStage)

        val expectedJson = """
            |{
            |  "assessment" : [ {
            |    "display" : "assessment"
            |  } ]
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "summary" : {
            |    "text" : "summary"
            |  }
            |}""".trimMargin()
        val conditionStage = objectMapper.readValue<ConditionStage>(json)

        assertNull(conditionStage.id)
        assertEquals(listOf<Extension>(), conditionStage.extension)
        assertEquals(listOf<Extension>(), conditionStage.modifierExtension)
        assertEquals(CodeableConcept(text = "summary"), conditionStage.summary)
        assertEquals(listOf<Reference>(), conditionStage.assessment)
        assertNull(conditionStage.type)
    }
}
