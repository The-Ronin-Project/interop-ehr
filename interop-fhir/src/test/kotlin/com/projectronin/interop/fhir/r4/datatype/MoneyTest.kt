package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MoneyTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val money = Money(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            value = 5.99,
            currency = Code("USD")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(money)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "value" : 5.99,
            |  "currency" : "USD"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedMoney = objectMapper.readValue<Money>(json)
        assertEquals(money, deserializedMoney)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val money = Money(value = 3.24)
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(money)

        val expectedJson = """
            |{
            |  "value" : 3.24
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "currency" : "USD"
            |}""".trimMargin()
        val money = objectMapper.readValue<Money>(json)

        assertNull(money.id)
        assertEquals(listOf<Extension>(), money.extension)
        assertNull(money.value)
        assertEquals(Code("USD"), money.currency)
    }
}
