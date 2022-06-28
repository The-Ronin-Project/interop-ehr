package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.SimpleQuantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicSimpleQuantityTest {
    @Test
    fun `can build from object`() {
        val simpleQuantity = SimpleQuantity(
            value = 1.0,
            unit = "in",
            system = Uri("system"),
            code = Code("code")
        )

        val epicSimpleQuantity = EpicSimpleQuantity(simpleQuantity)
        assertEquals(simpleQuantity, epicSimpleQuantity.element)
        assertEquals(1.0, epicSimpleQuantity.value)
        assertEquals("in", epicSimpleQuantity.unit)
        assertEquals("system", epicSimpleQuantity.system)
        assertEquals("code", epicSimpleQuantity.code)
    }

    @Test
    fun `supports no value`() {
        val simpleQuantity = SimpleQuantity(
            unit = "in",
            system = Uri("system"),
            code = Code("code")
        )

        val epicSimpleQuantity = EpicSimpleQuantity(simpleQuantity)
        assertEquals(simpleQuantity, epicSimpleQuantity.element)
        assertNull(epicSimpleQuantity.value)
        assertEquals("in", epicSimpleQuantity.unit)
        assertEquals("system", epicSimpleQuantity.system)
        assertEquals("code", epicSimpleQuantity.code)
    }

    @Test
    fun `supports no unit`() {
        val simpleQuantity = SimpleQuantity(
            value = 1.0,
            system = Uri("system"),
            code = Code("code")
        )

        val epicSimpleQuantity = EpicSimpleQuantity(simpleQuantity)
        assertEquals(simpleQuantity, epicSimpleQuantity.element)
        assertEquals(1.0, epicSimpleQuantity.value)
        assertNull(epicSimpleQuantity.unit)
        assertEquals("system", epicSimpleQuantity.system)
        assertEquals("code", epicSimpleQuantity.code)
    }

    @Test
    fun `supports no code`() {
        val simpleQuantity = SimpleQuantity(
            value = 1.0,
            unit = "in",
            system = Uri("system")
        )

        val epicSimpleQuantity = EpicSimpleQuantity(simpleQuantity)
        assertEquals(simpleQuantity, epicSimpleQuantity.element)
        assertEquals(1.0, epicSimpleQuantity.value)
        assertEquals("in", epicSimpleQuantity.unit)
        assertEquals("system", epicSimpleQuantity.system)
        assertNull(epicSimpleQuantity.code)
    }

    @Test
    fun `returns JSON as raw`() {
        val simpleQuantity = SimpleQuantity(
            value = 1.0,
            unit = "in",
            system = Uri("system"),
            code = Code("code")
        )

        val json = """{
          |  "value": 1.0,
          |  "unit": "in",
          |  "system": "system",
          |  "code": "code"
          |}""".trimMargin()

        val epicSimpleQuantity = EpicSimpleQuantity(simpleQuantity)
        assertEquals(simpleQuantity, epicSimpleQuantity.element)
        assertEquals(deformat(json), epicSimpleQuantity.raw)
    }
}
