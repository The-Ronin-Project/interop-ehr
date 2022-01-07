package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RatioTest {
    @Test
    fun `fails if non-null numerator and null denominator`() {
        val exception =
            assertThrows<IllegalArgumentException> { Ratio(numerator = Quantity(value = 3.0)) }
        assertEquals("denominator required when numerator present", exception.message)
    }

    @Test
    fun `fails if null numerator and non-null denominator`() {
        val exception =
            assertThrows<IllegalArgumentException> { Ratio(denominator = Quantity(value = 3.0)) }
        assertEquals("numerator required when denominator present", exception.message)
    }

    @Test
    fun `fails if no numerator or denominator and no extension`() {
        val exception = assertThrows<IllegalArgumentException> { Ratio() }
        assertEquals("extension required if no numerator and denominator", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val ratio = Ratio(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            numerator = Quantity(value = 3.0),
            denominator = Quantity(value = 4.0)
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ratio)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "numerator" : {
            |    "value" : 3.0
            |  },
            |  "denominator" : {
            |    "value" : 4.0
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedRatio = objectMapper.readValue<Ratio>(json)
        assertEquals(ratio, deserializedRatio)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val ratio = Ratio(numerator = Quantity(value = 3.0), denominator = Quantity(value = 4.0))
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ratio)

        val expectedJson = """
            |{
            |  "numerator" : {
            |    "value" : 3.0
            |  },
            |  "denominator" : {
            |    "value" : 4.0
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ]
            |}""".trimMargin()
        val ratio = objectMapper.readValue<Ratio>(json)

        assertNull(ratio.id)

        val expectedExtension =
            Extension(url = Uri("http://localhost/extension"), value = DynamicValue(DynamicValueType.STRING, "Value"))
        assertEquals(listOf(expectedExtension), ratio.extension)

        assertNull(ratio.numerator)
        assertNull(ratio.denominator)
    }
}
