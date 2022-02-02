package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.valueset.NameUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicHumanNameTest {
    @Test
    fun `can build from object`() {
        val humanName = HumanName(
            use = NameUse.OLD,
            text = "MYCHART,ALI",
            family = "Mychart",
            given = listOf("Ali")
        )

        val epicHumanName = EpicHumanName(humanName)
        assertEquals(humanName, epicHumanName.element)
        assertEquals(NameUse.OLD, epicHumanName.use)
        assertEquals("Mychart", epicHumanName.family)
        assertEquals(listOf("Ali"), epicHumanName.given)
    }

    @Test
    fun `supports no use `() {
        val humanName = HumanName(
            text = "MYCHART,ALI",
            family = "Mychart",
            given = listOf("Ali")
        )

        val epicHumanName = EpicHumanName(humanName)
        assertEquals(humanName, epicHumanName.element)
        assertNull(epicHumanName.use)
        assertEquals("Mychart", epicHumanName.family)
        assertEquals(listOf("Ali"), epicHumanName.given)
    }

    @Test
    fun `supports no family name`() {
        val humanName = HumanName(
            use = NameUse.OLD,
            text = "MYCHART,ALI",
            given = listOf("Ali")
        )

        val epicHumanName = EpicHumanName(humanName)
        assertEquals(humanName, epicHumanName.element)
        assertEquals(NameUse.OLD, epicHumanName.use)
        assertNull(epicHumanName.family)
        assertEquals(listOf("Ali"), epicHumanName.given)
    }

    @Test
    fun `supports no given names`() {
        val humanName = HumanName(
            use = NameUse.OLD,
            text = "MYCHART,ALI",
            family = "Mychart"
        )

        val epicHumanName = EpicHumanName(humanName)
        assertEquals(humanName, epicHumanName.element)
        assertEquals(NameUse.OLD, epicHumanName.use)
        assertEquals("Mychart", epicHumanName.family)
        assertEquals(listOf<String>(), epicHumanName.given)
    }

    @Test
    fun `supports multiple given names`() {
        val humanName = HumanName(
            use = NameUse.OLD,
            text = "MYCHART,ALI",
            family = "Mychart",
            given = listOf("Ali", "Middle")
        )

        val epicHumanName = EpicHumanName(humanName)
        assertEquals(humanName, epicHumanName.element)
        assertEquals(NameUse.OLD, epicHumanName.use)
        assertEquals("Mychart", epicHumanName.family)
        assertEquals(listOf("Ali", "Middle"), epicHumanName.given)
    }

    @Test
    fun `returns JSON as raw`() {
        val humanName = HumanName(
            use = NameUse.OLD,
            text = "MYCHART,ALI",
            family = "Mychart",
            given = listOf("Ali")
        )
        val json = """{
          |  "use": "old",
          |  "text": "MYCHART,ALI",
          |  "family": "Mychart",
          |  "given": [
          |    "Ali"
          |  ]
          |}""".trimMargin()

        val epicHumanName = EpicHumanName(humanName)
        assertEquals(humanName, epicHumanName.element)
        assertEquals(deformat(json), epicHumanName.raw)
    }
}
