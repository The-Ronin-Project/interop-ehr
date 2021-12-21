package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SimpleQuantityTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val simpleQuantity = SimpleQuantity(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            value = 17.5,
            unit = "years",
            system = CodeSystem.UCUM.uri,
            code = Code("a")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simpleQuantity)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "value" : 17.5,
            |  "unit" : "years",
            |  "system" : "http://unitsofmeasure.org",
            |  "code" : "a"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedSimpleQuantity = objectMapper.readValue<SimpleQuantity>(json)
        assertEquals(simpleQuantity, deserializedSimpleQuantity)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val simpleQuantity = SimpleQuantity(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            value = 17.5,
            code = Code("a")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simpleQuantity)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "value" : 17.5,
            |  "code" : "a"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "value" : 20.32
            |}""".trimMargin()
        val simpleQuantity = objectMapper.readValue<SimpleQuantity>(json)

        assertNull(simpleQuantity.id)
        assertEquals(listOf<Extension>(), simpleQuantity.extension)
        assertEquals(20.32, simpleQuantity.value)
        assertNull(simpleQuantity.comparator)
        assertNull(simpleQuantity.unit)
        assertNull(simpleQuantity.system)
        assertNull(simpleQuantity.code)
    }
}
