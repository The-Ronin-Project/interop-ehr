package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Time
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AvailableTimeTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val availableTime = AvailableTime(
            id = "67890",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            daysOfWeek = listOf(Code("mon")),
            allDay = false,
            availableStartTime = Time("08:00:00"),
            availableEndTime = Time("16:00:00")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(availableTime)

        val expectedJson = """
            |{
            |  "id" : "67890",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "modifierExtension" : [ {
            |    "url" : "http://localhost/modifier-extension",
            |    "valueString" : "Value"
            |  } ],
            |  "daysOfWeek" : [ "mon" ],
            |  "allDay" : false,
            |  "availableStartTime" : "08:00:00",
            |  "availableEndTime" : "16:00:00"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedAvailableTime = objectMapper.readValue<AvailableTime>(json)
        assertEquals(availableTime, deserializedAvailableTime)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val availableTime = AvailableTime(
            availableStartTime = Time("08:00:00")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(availableTime)

        val expectedJson = """
            |{
            |  "availableStartTime" : "08:00:00"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "allDay" : true
            |}""".trimMargin()
        val availableTime = objectMapper.readValue<AvailableTime>(json)

        assertNull(availableTime.id)
        assertEquals(listOf<Extension>(), availableTime.extension)
        assertEquals(listOf<Extension>(), availableTime.modifierExtension)
        assertEquals(listOf<Code>(), availableTime.daysOfWeek)
        assertEquals(true, availableTime.allDay)
        assertNull(availableTime.availableStartTime)
        assertNull(availableTime.availableEndTime)
    }
}
