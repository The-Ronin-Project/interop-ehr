package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RangeTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val range = Range(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            low = SimpleQuantity(value = 1.0),
            high = SimpleQuantity(value = 10.3)
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(range)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "low" : {
            |    "value" : 1.0
            |  },
            |  "high" : {
            |    "value" : 10.3
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedRange = objectMapper.readValue<Range>(json)
        assertEquals(range, deserializedRange)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val range = Range(low = SimpleQuantity(value = 1.0))
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(range)

        val expectedJson = """
            |{
            |  "low" : {
            |    "value" : 1.0
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "high" : {
            |    "value" : 10.3
            |  }
            |}""".trimMargin()
        val range = objectMapper.readValue<Range>(json)

        assertNull(range.id)
        assertEquals(listOf<Extension>(), range.extension)
        assertNull(range.low)
        assertEquals(SimpleQuantity(value = 10.3), range.high)
    }
}
