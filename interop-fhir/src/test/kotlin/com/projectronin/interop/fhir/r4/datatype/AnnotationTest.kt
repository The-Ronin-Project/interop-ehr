package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AnnotationTest {
    @Test
    fun `fails for unsupported author dynamic value type`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Annotation(
                    author = DynamicValue(DynamicValueType.INTEGER, 1),
                    text = Markdown("text")
                )
            }
        assertEquals("author can only be one of the following: Reference, String", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val annotation = Annotation(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            author = DynamicValue(DynamicValueType.STRING, "Author"),
            time = DateTime("2021-11-17"),
            text = Markdown("Markdown text")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(annotation)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "authorString" : "Author",
            |  "time" : "2021-11-17",
            |  "text" : "Markdown text"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedAnnotation = objectMapper.readValue<Annotation>(json)
        assertEquals(annotation, deserializedAnnotation)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val annotation = Annotation(text = Markdown("Markdown text"))
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(annotation)

        val expectedJson = """
            |{
            |  "text" : "Markdown text"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "text" : "Markdown text"
            |}""".trimMargin()
        val annotation = objectMapper.readValue<Annotation>(json)

        assertNull(annotation.id)
        assertEquals(listOf<Extension>(), annotation.extension)
        assertNull(annotation.author)
        assertNull(annotation.time)
        assertEquals(Markdown("Markdown text"), annotation.text)
    }
}
