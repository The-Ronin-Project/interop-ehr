package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.fhir.r4.valueset.NameUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicHumanNameTest {
    @Test
    fun `can build from JSON`() {
        val json = """{
          |  "use": "old",
          |  "text": "MYCHART,ALI",
          |  "family": "Mychart",
          |  "given": [
          |    "Ali"
          |  ]
          |}""".trimMargin()

        val name = EpicHumanName(json)
        assertEquals(json, name.raw)
        assertEquals(NameUse.OLD, name.use)
        assertEquals("Mychart", name.family)
        assertEquals(listOf("Ali"), name.given)
    }

    @Test
    fun `supports no use `() {
        val json = """{
          |  "text": "MYCHART,ALI",
          |  "family": "Mychart",
          |  "given": [
          |    "Ali"
          |  ]
          |}""".trimMargin()

        val name = EpicHumanName(json)
        assertEquals(json, name.raw)
        assertNull(name.use)
        assertEquals("Mychart", name.family)
        assertEquals(listOf("Ali"), name.given)
    }

    @Test
    fun `supports no family name`() {
        val json = """{
          |  "use": "old",
          |  "text": "MYCHART,ALI",
          |  "given": [
          |    "Ali"
          |  ]
          |}""".trimMargin()

        val name = EpicHumanName(json)
        assertEquals(json, name.raw)
        assertEquals(NameUse.OLD, name.use)
        assertNull(name.family)
        assertEquals(listOf("Ali"), name.given)
    }

    @Test
    fun `supports no given names`() {
        val json = """{
          |  "use": "old",
          |  "text": "MYCHART,ALI",
          |  "family": "Mychart"
          |}""".trimMargin()

        val name = EpicHumanName(json)
        assertEquals(json, name.raw)
        assertEquals(NameUse.OLD, name.use)
        assertEquals("Mychart", name.family)
        assertEquals(listOf<String>(), name.given)
    }

    @Test
    fun `supports multiple given names`() {
        val json = """{
          |  "use": "old",
          |  "text": "MYCHART,ALI",
          |  "family": "Mychart",
          |  "given": [
          |    "Ali",
          |    "Middle"
          |  ]
          |}""".trimMargin()

        val name = EpicHumanName(json)
        assertEquals(json, name.raw)
        assertEquals(NameUse.OLD, name.use)
        assertEquals("Mychart", name.family)
        assertEquals(listOf("Ali", "Middle"), name.given)
    }
}
