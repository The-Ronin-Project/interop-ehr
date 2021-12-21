package com.projectronin.interop.ehr.epic.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicIdentifierTest {
    @Test
    fun `can build from JSON`() {
        val json = """{
          |  "use": "usual",
          |  "system": "urn:oid:2.16.840.1.113883.4.1",
          |  "value": "391-50-5316"
          |}""".trimMargin()

        val identifier = EpicIdentifier(json)
        assertEquals(json, identifier.raw)
        assertEquals("urn:oid:2.16.840.1.113883.4.1", identifier.system)
        assertEquals("391-50-5316", identifier.value)
    }

    @Test
    fun `can handle no value from JSON`() {
        val json = """{
          |  "use": "usual",
          |  "system": "urn:oid:2.16.840.1.113883.4.1",
          |}""".trimMargin()

        val identifier = EpicIdentifier(json)
        assertEquals(json, identifier.raw)
        assertEquals("urn:oid:2.16.840.1.113883.4.1", identifier.system)
        assertEquals("", identifier.value)
    }
}
