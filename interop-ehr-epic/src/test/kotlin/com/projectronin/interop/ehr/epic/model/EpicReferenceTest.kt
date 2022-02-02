package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicReferenceTest {
    @Test
    fun `can build from object`() {
        val reference = Reference(
            reference = "Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3",
            display = "John Adams, MD"
        )

        val epicReference = EpicReference(reference)
        assertEquals(reference, epicReference.element)
        assertEquals("Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3", epicReference.reference)
        assertEquals("John Adams, MD", epicReference.display)
    }

    @Test
    fun `can build with a null display`() {
        val reference = Reference(
            reference = "Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3"
        )

        val epicReference = EpicReference(reference)
        assertEquals(reference, epicReference.element)
        assertEquals("Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3", epicReference.reference)
        assertNull(epicReference.display)
    }

    @Test
    fun `returns JSON as raw`() {
        val reference = Reference(
            reference = "Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3",
            display = "John Adams, MD"
        )
        val json = """{
            |  "reference": "Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3",
            |  "display": "John Adams, MD"
            |}""".trimMargin()

        val epicReference = EpicReference(reference)
        assertEquals(reference, epicReference.element)
        assertEquals(deformat(json), epicReference.raw)
    }
}
