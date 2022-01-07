package com.projectronin.interop.fhir.epic

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdTypeTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val idType = IdType(
            id = "123",
            type = "abc"
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(idType)

        val expectedJson = """
            {
              "ID" : "123",
              "Type" : "abc"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedIdType = JacksonManager.objectMapper.readValue<IdType>(json)
        assertEquals(idType, deserializedIdType)
    }
}
