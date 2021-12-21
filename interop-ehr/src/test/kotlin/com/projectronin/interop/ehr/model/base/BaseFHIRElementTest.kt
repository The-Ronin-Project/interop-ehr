package com.projectronin.interop.ehr.model.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaseFHIRElementTest {
    @Test
    fun `creates json object`() {
        val json = """{"id":1,"name":"Name"}"""
        val element = SampleFHIRElement(json)

        // The jsonObject is not in scope here, so we're using a method to expose its details.
        assertEquals(json, element.rawJson())
        assertEquals(json, element.raw)
    }

    @Test
    fun `equals and hashCode work`() {
        val json = """{"id":1,"name":"Name"}"""
        val element1 = SampleFHIRElement(json)
        val element2 = SampleFHIRElement(json)
        val element3 = SampleFHIRElement("{}")

        assertTrue(element1 == element1)
        assertFalse(element1.equals(null))
        assertTrue(element1 == element2)
        assertTrue(element1 != element3)
        assertFalse(element1.equals(json))
        assertTrue(element1.hashCode() == element2.hashCode())
    }
}

class SampleFHIRElement(private val rawString: String) : FHIRElement(rawString) {
    fun rawJson() = jsonObject.toJsonString()
}
