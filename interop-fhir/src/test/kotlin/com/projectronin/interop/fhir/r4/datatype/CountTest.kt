package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.QuantityComparator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CountTest {
    @Test
    fun `fails if value provided without code`() {
        val exception = assertThrows<IllegalArgumentException> { Count(value = 2.0) }
        assertEquals("There SHALL be a code with a value of \"1\" if there is a value", exception.message)
    }

    @Test
    fun `fails if value provided with invalid code`() {
        val exception = assertThrows<IllegalArgumentException> { Count(value = 2.0, code = Code("code-value")) }
        assertEquals("There SHALL be a code with a value of \"1\" if there is a value", exception.message)
    }

    @Test
    fun `fails if system is provided and not UCUM`() {
        val exception = assertThrows<IllegalArgumentException> { Count(system = Uri("SNOMED")) }
        assertEquals("If system is present, it SHALL be UCUM", exception.message)
    }

    @Test
    fun `fails if value is non-whole number`() {
        val exception = assertThrows<IllegalArgumentException> { Count(code = Code("1"), value = 1.2) }
        assertEquals("If present, the value SHALL be a whole number", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val count = Count(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            value = 17.0,
            comparator = QuantityComparator.GREATER_OR_EQUAL_TO,
            unit = "1",
            system = CodeSystem.UCUM.uri,
            code = Code("1")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(count)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "value" : 17.0,
            |  "comparator" : ">=",
            |  "unit" : "1",
            |  "system" : "http://unitsofmeasure.org",
            |  "code" : "1"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedCount = objectMapper.readValue<Count>(json)
        assertEquals(count, deserializedCount)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val count = Count(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            value = 17.0,
            code = Code("1")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(count)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "value" : 17.0,
            |  "code" : "1"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "comparator" : ">="
            |}""".trimMargin()
        val count = objectMapper.readValue<Count>(json)

        Assertions.assertNull(count.id)
        assertEquals(listOf<Extension>(), count.extension)
        Assertions.assertNull(count.value)
        assertEquals(QuantityComparator.GREATER_OR_EQUAL_TO, count.comparator)
        Assertions.assertNull(count.unit)
        Assertions.assertNull(count.system)
        Assertions.assertNull(count.code)
    }
}
