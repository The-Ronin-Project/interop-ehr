package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CodeableConceptTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val codeableConcept = CodeableConcept(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            coding = listOf(Coding(system = Uri("coding-system"))),
            text = "concept"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(codeableConcept)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "coding" : [ {
            |    "system" : "coding-system"
            |  } ],
            |  "text" : "concept"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedCodeableConcept = objectMapper.readValue<CodeableConcept>(json)
        assertEquals(codeableConcept, deserializedCodeableConcept)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val codeableConcept = CodeableConcept(
            text = "concept"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(codeableConcept)

        val expectedJson = """
            |{
            |  "text" : "concept"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "id" : "12345"
            |}""".trimMargin()
        val codeableConcept = objectMapper.readValue<CodeableConcept>(json)

        assertEquals("12345", codeableConcept.id)
        assertEquals(listOf<Extension>(), codeableConcept.extension)
        assertEquals(listOf<Coding>(), codeableConcept.coding)
        assertNull(codeableConcept.text)
    }
}
