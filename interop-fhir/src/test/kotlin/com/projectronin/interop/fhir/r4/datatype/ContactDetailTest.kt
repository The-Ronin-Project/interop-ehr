package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ContactDetailTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val contactDetail = ContactDetail(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            name = "Jane Doe",
            telecom = listOf(ContactPoint(value = "jane@doe.com"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contactDetail)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "name" : "Jane Doe",
            |  "telecom" : [ {
            |    "value" : "jane@doe.com"
            |  } ]
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedContactDetail = objectMapper.readValue<ContactDetail>(json)
        assertEquals(contactDetail, deserializedContactDetail)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val contactDetail = ContactDetail(
            name = "Jane Doe"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contactDetail)

        val expectedJson = """
            |{
            |  "name" : "Jane Doe"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "id" : "12345"
            |}""".trimMargin()
        val contactDetail = objectMapper.readValue<ContactDetail>(json)

        assertEquals("12345", contactDetail.id)
        assertEquals(listOf<Extension>(), contactDetail.extension)
        assertNull(contactDetail.name)
        assertEquals(listOf<ContactPoint>(), contactDetail.telecom)
    }
}
