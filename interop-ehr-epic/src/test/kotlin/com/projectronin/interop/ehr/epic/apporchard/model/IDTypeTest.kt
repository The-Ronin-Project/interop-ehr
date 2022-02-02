package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IDTypeTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val idType = IDType(
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

        val deserializedIDType = JacksonManager.objectMapper.readValue<IDType>(json)
        assertEquals(idType, deserializedIDType)
    }
}
