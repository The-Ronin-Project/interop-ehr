package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TriggerDefinitionTest {
    @Test
    fun `fails for unsupported author dynamic value type`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                TriggerDefinition(type = Code("type"), timing = DynamicValue(DynamicValueType.INTEGER, 1))
            }
        assertEquals("timing can only be one of the following: Timing, Reference, Date, DateTime", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val triggerDefinition = TriggerDefinition(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            type = Code("type-code"),
            name = "trigger name",
            timing = DynamicValue(DynamicValueType.DATE, Date("2022")),
            data = listOf(DataRequirement(type = Code("data-type-code"))),
            condition = Expression(language = Code("en-US"), expression = "Expression")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(triggerDefinition)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "type" : "type-code",
            |  "name" : "trigger name",
            |  "timingDate" : "2022",
            |  "data" : [ {
            |    "type" : "data-type-code"
            |  } ],
            |  "condition" : {
            |    "language" : "en-US",
            |    "expression" : "Expression"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedTriggerDefinition = objectMapper.readValue<TriggerDefinition>(json)
        assertEquals(triggerDefinition, deserializedTriggerDefinition)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val triggerDefinition = TriggerDefinition(
            type = Code("type-code")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(triggerDefinition)

        val expectedJson = """
            |{
            |  "type" : "type-code"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "type" : "type-code"
            |}""".trimMargin()
        val triggerDefinition = objectMapper.readValue<TriggerDefinition>(json)

        assertNull(triggerDefinition.id)
        assertEquals(listOf<Extension>(), triggerDefinition.extension)
        assertEquals(Code("type-code"), triggerDefinition.type)
        assertNull(triggerDefinition.name)
        assertNull(triggerDefinition.timing)
        assertEquals(listOf<DataRequirement>(), triggerDefinition.data)
        assertNull(triggerDefinition.condition)
    }
}
