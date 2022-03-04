package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.exception.UnsupportedDynamicValueTypeException
import com.projectronin.interop.ehr.model.Annotation.ReferenceAuthor
import com.projectronin.interop.ehr.model.Annotation.StringAuthor
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicAnnotationTest {
    @Test
    fun `can build from object`() {
        val annotation = Annotation(
            author = DynamicValue(DynamicValueType.STRING, "Author"),
            time = DateTime("2022-03-03"),
            text = Markdown("text")
        )

        val epicAnnotation = EpicAnnotation(annotation)
        assertEquals(annotation, epicAnnotation.element)
        assertNotNull(epicAnnotation.author)
        assertEquals("2022-03-03", epicAnnotation.time)
        assertEquals("text", epicAnnotation.text)
    }

    @Test
    fun `supports no time`() {
        val annotation = Annotation(
            author = DynamicValue(DynamicValueType.STRING, "Author"),
            text = Markdown("text")
        )

        val epicAnnotation = EpicAnnotation(annotation)
        assertEquals(annotation, epicAnnotation.element)
        assertNotNull(epicAnnotation.author)
        assertNull(epicAnnotation.time)
        assertEquals("text", epicAnnotation.text)
    }

    @Test
    fun `supports no author`() {
        val annotation = Annotation(
            time = DateTime("2022-03-03"),
            text = Markdown("text")
        )

        val epicAnnotation = EpicAnnotation(annotation)
        assertEquals(annotation, epicAnnotation.element)
        assertNull(epicAnnotation.author)
        assertEquals("2022-03-03", epicAnnotation.time)
        assertEquals("text", epicAnnotation.text)
    }

    @Test
    fun `supports author string`() {
        val annotation = Annotation(
            author = DynamicValue(DynamicValueType.STRING, "Author"),
            text = Markdown("text")
        )

        val epicAnnotation = EpicAnnotation(annotation)

        val author = epicAnnotation.author!! as StringAuthor
        assertEquals("Author", author.value)
    }

    @Test
    fun `supports author reference`() {
        val reference = Reference(display = "Author")
        val annotation = Annotation(
            author = DynamicValue(DynamicValueType.REFERENCE, reference),
            text = Markdown("text")
        )

        val epicAnnotation = EpicAnnotation(annotation)

        val author = epicAnnotation.author!! as ReferenceAuthor
        assertEquals(reference, author.value.element)
    }

    @Test
    fun `throws exception for unsupported author type`() {
        // We currently prohibit creating a Condition with an invalid type
        val annotation = mockk<Annotation>(relaxed = true) {
            every { author } returns DynamicValue(DynamicValueType.BOOLEAN, true)
        }
        val epicAnnotation = EpicAnnotation(annotation)

        val exception = assertThrows<UnsupportedDynamicValueTypeException> { epicAnnotation.author }
        assertEquals("BOOLEAN is not a supported type for annotation author", exception.message)
    }

    @Test
    fun `returns JSON as raw`() {
        val annotation = Annotation(
            author = DynamicValue(DynamicValueType.STRING, "Author"),
            time = DateTime("2022-03-03"),
            text = Markdown("text")
        )
        val json = """{
          |  "authorString": "Author",
          |  "time": "2022-03-03",
          |  "text": "text"
          |}""".trimMargin()

        val epicAnnotation = EpicAnnotation(annotation)
        assertEquals(annotation, epicAnnotation.element)
        assertEquals(deformat(json), epicAnnotation.raw)
    }
}
