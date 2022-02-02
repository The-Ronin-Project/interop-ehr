package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.deformat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicIDTypeTest {
    @Test
    fun `can build from object`() {
        val idType = IDType(id = "E5597", type = "ExternalKey")

        val epicIDType = EpicIDType(idType)
        assertEquals(idType, epicIDType.element)
        assertEquals("ExternalKey", epicIDType.system)
        assertEquals("E5597", epicIDType.value)
    }

    @Test
    fun `returns JSON as raw`() {
        val idType = IDType(id = "E5597", type = "ExternalKey")
        val json = """{
        |  "ID": "E5597",
        |  "Type": "ExternalKey"
        |}""".trimMargin()

        val epicIDType = EpicIDType(idType)
        assertEquals(idType, epicIDType.element)
        assertEquals(deformat(json), epicIDType.raw)
    }
}
