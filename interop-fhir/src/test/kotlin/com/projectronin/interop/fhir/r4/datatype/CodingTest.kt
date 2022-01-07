package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CodingTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val coding = Coding(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            system = Uri("system-uri"),
            version = "1.2",
            code = Code("code-value"),
            display = "code-value from system-uri",
            userSelected = true
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(coding)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "system" : "system-uri",
            |  "version" : "1.2",
            |  "code" : "code-value",
            |  "display" : "code-value from system-uri",
            |  "userSelected" : true
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedCoding = objectMapper.readValue<Coding>(json)
        assertEquals(coding, deserializedCoding)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val coding = Coding(
            display = "code-value from system-uri"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(coding)

        val expectedJson = """
            |{
            |  "display" : "code-value from system-uri"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "code" : "code-value"
            |}""".trimMargin()
        val coding = objectMapper.readValue<Coding>(json)

        assertNull(coding.id)
        assertEquals(listOf<Extension>(), coding.extension)
        assertNull(coding.system)
        assertNull(coding.version)
        assertEquals(Code("code-value"), coding.code)
        assertNull(coding.display)
        assertNull(coding.userSelected)
    }
}
