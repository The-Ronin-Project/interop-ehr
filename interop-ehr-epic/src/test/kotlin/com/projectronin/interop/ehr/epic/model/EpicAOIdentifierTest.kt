package com.projectronin.interop.ehr.epic.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicAOIdentifierTest {

    @Test
    fun `can build from JSON`() {
        val json = """{
        |  "ID": "E5597",
        |  "Type": "ExternalKey"
        |}""".trimMargin()

        val identifier = EpicIDType(json)
        assertEquals(json, identifier.raw)
        assertEquals("ExternalKey", identifier.system)
        assertEquals("E5597", identifier.value)
    }
}
