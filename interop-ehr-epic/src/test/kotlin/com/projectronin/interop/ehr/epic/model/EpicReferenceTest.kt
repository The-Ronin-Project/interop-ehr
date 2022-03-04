package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicReferenceTest {
    @Test
    fun `can build from object`() {
        val identifier = Identifier(id = "1234")
        val reference = Reference(
            id = "5678",
            reference = "Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3",
            type = Uri("type"),
            identifier = identifier,
            display = "John Adams, MD"
        )

        val epicReference = EpicReference(reference)
        assertEquals(reference, epicReference.element)
        assertEquals("Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3", epicReference.reference)
        assertEquals("John Adams, MD", epicReference.display)
        assertEquals("5678", epicReference.id)
        assertEquals(identifier, epicReference.identifier?.element)
        assertEquals("type", epicReference.type)
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
        assertNull(epicReference.id)
        assertNull(epicReference.identifier)
        assertNull(epicReference.type)
    }

    @Test
    fun `returns JSON as raw`() {
        val identifier = Identifier(id = "1234")
        val reference = Reference(
            id = "5678",
            reference = "Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3",
            type = Uri("type"),
            identifier = identifier,
            display = "John Adams, MD"
        )

        val identifierJSON = """{"id":"1234"}"""
        val json = """{
            |  "id": "5678",
            |  "reference": "Practitioner/eUqvfRVFNUVeJmt1pCf8YS24CEOUR6bhBnL-xrSXlTdc3",
            |  "type": "type",
            |  "identifier": $identifierJSON,
            |  "display": "John Adams, MD"
            |}""".trimMargin()

        val epicReference = EpicReference(reference)
        assertEquals(reference, epicReference.element)
        assertEquals(deformat(json), epicReference.raw)

        assertEquals(identifier, epicReference.identifier?.element)
        assertEquals(identifierJSON, epicReference.identifier?.raw)
    }
}
