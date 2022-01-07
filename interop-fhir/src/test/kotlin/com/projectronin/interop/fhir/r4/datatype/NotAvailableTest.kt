package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NotAvailableTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val notAvailable = NotAvailable(
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
            description = "Not available now",
            during = Period(start = DateTime("2021-12-01"), end = DateTime("2021-12-08"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(notAvailable)

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
            |  "description" : "Not available now",
            |  "during" : {
            |    "start" : "2021-12-01",
            |    "end" : "2021-12-08"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedNotAvailable = objectMapper.readValue<NotAvailable>(json)
        assertEquals(notAvailable, deserializedNotAvailable)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val notAvailable = NotAvailable(
            description = "Vacation"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(notAvailable)

        val expectedJson = """
            |{
            |  "description" : "Vacation"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "description" : "Vacation"
            |}""".trimMargin()
        val notAvailable = objectMapper.readValue<NotAvailable>(json)

        assertNull(notAvailable.id)
        assertEquals(listOf<Extension>(), notAvailable.extension)
        assertEquals(listOf<Extension>(), notAvailable.modifierExtension)
        assertEquals("Vacation", notAvailable.description)
        assertNull(notAvailable.during)
    }
}
