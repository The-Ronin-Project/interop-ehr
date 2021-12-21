package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExpressionTest {
    @Test
    fun `fails if no expression or reference`() {
        val exception = assertThrows<IllegalArgumentException> { Expression(language = Code("en-US")) }
        assertEquals("An expression or a reference must be provided", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val expression = Expression(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            description = "expression 1",
            name = Id("12-45"),
            language = Code("en-US"),
            expression = "the expression",
            reference = Uri("expression-uri")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expression)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "description" : "expression 1",
            |  "name" : "12-45",
            |  "language" : "en-US",
            |  "expression" : "the expression",
            |  "reference" : "expression-uri"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedExpression = objectMapper.readValue<Expression>(json)
        assertEquals(expression, deserializedExpression)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val expression = Expression(
            language = Code("en-US"),
            expression = "expression"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expression)

        val expectedJson = """
            |{
            |  "language" : "en-US",
            |  "expression" : "expression"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "language" : "en-US",
            |  "reference" : "reference"
            |}""".trimMargin()
        val expression = objectMapper.readValue<Expression>(json)

        assertNull(expression.id)
        assertEquals(listOf<Extension>(), expression.extension)
        assertNull(expression.description)
        assertNull(expression.name)
        assertEquals(Code("en-US"), expression.language)
        assertNull(expression.expression)
        assertEquals(Uri("reference"), expression.reference)
    }
}
