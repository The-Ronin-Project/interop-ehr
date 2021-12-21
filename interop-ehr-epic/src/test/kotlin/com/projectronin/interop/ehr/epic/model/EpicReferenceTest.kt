package com.projectronin.interop.ehr.epic.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicReferenceTest {
    @Test
    fun `can build from JSON`() {
        val json = """{
            |  "reference": "Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3",
            |  "display": "John Adams, MD"
            |}""".trimMargin()

        val reference = EpicReference(json)
        assertEquals(json, reference.raw)
        assertEquals("Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3", reference.reference)
        assertEquals("John Adams, MD", reference.display)
    }

    @Test
    fun `can build with a null display`() {
        val json = """{
            |  "reference": "Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3",
            |}""".trimMargin()

        val reference = EpicReference(json)
        assertEquals(json, reference.raw)
        assertEquals("Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3", reference.reference)
        assertNull(reference.display)
    }
}
