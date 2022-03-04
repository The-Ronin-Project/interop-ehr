package com.projectronin.interop.ehr.model

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnnotationTest {
    @Test
    fun `creates reference author`() {
        val reference = mockk<Reference>()
        val author = Annotation.ReferenceAuthor(reference)
        assertEquals(reference, author.value)
    }

    @Test
    fun `creates string author`() {
        val author = Annotation.StringAuthor("Stephen King")
        assertEquals("Stephen King", author.value)
    }
}
