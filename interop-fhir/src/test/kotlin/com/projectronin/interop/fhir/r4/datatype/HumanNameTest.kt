package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.NameUse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HumanNameTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val humanName = HumanName(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            use = NameUse.OFFICIAL,
            text = "Jane Doe",
            family = "Doe",
            given = listOf("Jane"),
            prefix = listOf("Dr"),
            suffix = listOf("M.D."),
            period = Period(start = DateTime("1994-02-29"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(humanName)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "use" : "official",
            |  "text" : "Jane Doe",
            |  "family" : "Doe",
            |  "given" : [ "Jane" ],
            |  "prefix" : [ "Dr" ],
            |  "suffix" : [ "M.D." ],
            |  "period" : {
            |    "start" : "1994-02-29"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedHumanName = objectMapper.readValue<HumanName>(json)
        assertEquals(humanName, deserializedHumanName)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val humanName = HumanName(
            text = "Name"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(humanName)

        val expectedJson = """
            |{
            |  "text" : "Name"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "use" : "nickname"
            |}""".trimMargin()
        val humanName = objectMapper.readValue<HumanName>(json)

        Assertions.assertNull(humanName.id)
        assertEquals(listOf<Extension>(), humanName.extension)
        assertEquals(NameUse.NICKNAME, humanName.use)
        assertNull(humanName.text)
        assertNull(humanName.family)
        assertEquals(listOf<String>(), humanName.given)
        assertEquals(listOf<String>(), humanName.prefix)
        assertEquals(listOf<String>(), humanName.suffix)
        assertNull(humanName.period)
    }
}
