package com.projectronin.interop.ehr.model.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JSONElementTest {
    @Test
    fun `creates json object`() {
        val elementObject = "Bundle String"
        val element = SampleJSONElement(elementObject)

        assertEquals("\"$elementObject\"", element.raw)
        assertEquals(elementObject, element.element)
    }

    @Test
    fun `equals and hashCode work`() {
        val elementObject = "Bundle String"
        val element1 = SampleJSONElement(elementObject)
        val element2 = SampleJSONElement(elementObject)
        val element3 = SampleJSONElement("Other")

        assertTrue(element1 == element1)
        assertFalse(element1.equals(null))
        assertTrue(element1 == element2)
        assertTrue(element1 != element3)
        assertFalse(element1.equals(elementObject))
        assertTrue(element1.hashCode() == element2.hashCode())
    }
}

class SampleJSONElement(element: Any) : JSONElement(element)
