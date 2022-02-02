package com.projectronin.interop.ehr.model.helper

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.projectronin.interop.fhir.r4.valueset.NameUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JsonFhirHelpersTest {
    @Test
    fun `enum when field not found`() {
        val jsonObject = jsonObject("{}")
        val enum = jsonObject.enum<NameUse>("name")
        assertNull(enum)
    }

    @Test
    fun `enum when field has non valid code`() {
        val jsonObject = jsonObject("""{"name": "unknown"}""")
        val enum = jsonObject.enum<NameUse>("name")
        assertNull(enum)
    }

    @Test
    fun `enum when field has mapped code`() {
        val jsonObject = jsonObject("""{"name": "official"}""")
        val enum = jsonObject.enum<NameUse>("name")
        assertEquals(NameUse.OFFICIAL, enum)
    }

    @Test
    fun `enum when field has supplemental mapped code`() {
        val jsonObject = jsonObject("""{"name": "offi"}""")
        val enum = jsonObject.enum("name", mapOf("offi" to NameUse.OFFICIAL))
        assertEquals(NameUse.OFFICIAL, enum)
    }

    @Test
    fun `enum when field has supplemental miss but default match`() {
        val jsonObject = jsonObject("""{"name": "official"}""")
        val enum = jsonObject.enum("name", mapOf("offi" to NameUse.OFFICIAL))
        assertEquals(NameUse.OFFICIAL, enum)
    }

    @Test
    fun `enum when field with mapping has default value`() {
        val jsonObject = jsonObject("""{"name": "self"}""")
        val enum = jsonObject.enum("name", mapOf("offi" to NameUse.OFFICIAL), NameUse.OLD)
        assertEquals(NameUse.OLD, enum)
    }

    @Test
    fun `enum when field has default value`() {
        val jsonObject = jsonObject("""{"name": "self"}""")
        val enum = jsonObject.enum("name", NameUse.OLD)
        assertEquals(NameUse.OLD, enum)
    }

    @Test
    fun `enum when field no default`() {
        "x".let { }
        val jsonObject = jsonObject("""{"name": "self"}""")
        val enum = jsonObject.enum("name", mapOf("offi" to NameUse.OFFICIAL))
        assertNull(enum)
    }

    @Test
    fun `enum when field null default`() {
        val jsonObject = jsonObject("""{"name": "self"}""")
        val enum = jsonObject.enum("name", mapOf("offi" to NameUse.OFFICIAL), null)
        assertNull(enum)
    }

    private fun jsonObject(json: String) = Parser.default().parse(StringBuilder(json)) as JsonObject
}
