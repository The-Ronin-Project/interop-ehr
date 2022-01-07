package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.QuantityComparator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DistanceTest {
    @Test
    fun `fails if value provided without code`() {
        val exception = assertThrows<IllegalArgumentException> { Distance(value = 2.0) }
        assertEquals("There SHALL be a code if there is a value", exception.message)
    }

    @Test
    fun `fails if system is provided and not UCUM`() {
        val exception = assertThrows<IllegalArgumentException> { Distance(system = Uri("SNOMED")) }
        assertEquals("If system is present, it SHALL be UCUM", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val distance = Distance(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            value = 17.5,
            comparator = QuantityComparator.GREATER_OR_EQUAL_TO,
            unit = "millimeters",
            system = CodeSystem.UCUM.uri,
            code = Code("mm")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(distance)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "value" : 17.5,
            |  "comparator" : ">=",
            |  "unit" : "millimeters",
            |  "system" : "http://unitsofmeasure.org",
            |  "code" : "mm"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedDistance = objectMapper.readValue<Distance>(json)
        assertEquals(distance, deserializedDistance)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val distance = Distance(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            value = 17.5,
            code = Code("mm")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(distance)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "value" : 17.5,
            |  "code" : "mm"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "comparator" : ">="
            |}""".trimMargin()
        val distance = objectMapper.readValue<Distance>(json)

        assertNull(distance.id)
        assertEquals(listOf<Extension>(), distance.extension)
        assertNull(distance.value)
        assertEquals(QuantityComparator.GREATER_OR_EQUAL_TO, distance.comparator)
        assertNull(distance.unit)
        assertNull(distance.system)
        assertNull(distance.code)
    }
}
