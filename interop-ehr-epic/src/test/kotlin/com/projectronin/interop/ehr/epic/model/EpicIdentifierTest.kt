package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicIdentifierTest {
    @Test
    fun `can build from object`() {
        val identifier = Identifier(
            use = IdentifierUse.USUAL,
            type = CodeableConcept(text = "NPI"),
            system = Uri("urn:oid:2.16.840.1.113883.4.1"),
            value = "391-50-5316"
        )

        val epicIdentifier = EpicIdentifier(identifier)
        assertEquals(identifier, epicIdentifier.element)
        assertEquals("NPI", epicIdentifier.type?.text)
        assertEquals("urn:oid:2.16.840.1.113883.4.1", epicIdentifier.system)
        assertEquals("391-50-5316", epicIdentifier.value)
    }

    @Test
    fun `can handle no value from JSON`() {
        val identifier = Identifier(
            use = IdentifierUse.USUAL,
            type = CodeableConcept(text = "NPI"),
            system = Uri("urn:oid:2.16.840.1.113883.4.1")
        )

        val epicIdentifier = EpicIdentifier(identifier)
        assertEquals(identifier, epicIdentifier.element)
        assertEquals("NPI", epicIdentifier.type?.text)
        assertEquals("urn:oid:2.16.840.1.113883.4.1", epicIdentifier.system)
        assertEquals("", epicIdentifier.value)
    }

    @Test
    fun `returns JSON as raw`() {
        val identifier = Identifier(
            use = IdentifierUse.USUAL,
            type = CodeableConcept(text = "NPI"),
            system = Uri("urn:oid:2.16.840.1.113883.4.1"),
            value = "391-50-5316"
        )
        val json = """{
          |  "use": "usual",
          |  "type": { "text": "NPI" },
          |  "system": "urn:oid:2.16.840.1.113883.4.1",
          |  "value": "391-50-5316"
          |}""".trimMargin()

        val epicIdentifier = EpicIdentifier(identifier)
        assertEquals(identifier, epicIdentifier.element)
        assertEquals(deformat(json), epicIdentifier.raw)
    }
}
