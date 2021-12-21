package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SampledDataTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val sampledData = SampledData(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            origin = SimpleQuantity(value = 3.0),
            period = 2.0,
            factor = 4.0,
            lowerLimit = 0.0,
            upperLimit = 205.4,
            dimensions = PositiveInt(3),
            data = "the data"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sampledData)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "origin" : {
            |    "value" : 3.0
            |  },
            |  "period" : 2.0,
            |  "factor" : 4.0,
            |  "lowerLimit" : 0.0,
            |  "upperLimit" : 205.4,
            |  "dimensions" : 3,
            |  "data" : "the data"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedSampledData = objectMapper.readValue<SampledData>(json)
        assertEquals(sampledData, deserializedSampledData)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val sampledData = SampledData(origin = SimpleQuantity(value = 1.0), period = 3.0, dimensions = PositiveInt(5))
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sampledData)

        val expectedJson = """
            |{
            |  "origin" : {
            |    "value" : 1.0
            |  },
            |  "period" : 3.0,
            |  "dimensions" : 5
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "origin" : {
            |    "value" : 1.0
            |  },
            |  "period" : 3.0,
            |  "dimensions" : 5
            |}""".trimMargin()
        val sampledData = objectMapper.readValue<SampledData>(json)

        assertNull(sampledData.id)
        assertEquals(listOf<Extension>(), sampledData.extension)
        assertEquals(SimpleQuantity(value = 1.0), sampledData.origin)
        assertEquals(3.0, sampledData.period)
        assertNull(sampledData.factor)
        assertNull(sampledData.lowerLimit)
        assertNull(sampledData.upperLimit)
        assertEquals(PositiveInt(5), sampledData.dimensions)
        assertNull(sampledData.data)
    }
}
