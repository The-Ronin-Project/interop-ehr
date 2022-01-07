package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.ContributorType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ContributorTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val contributor = Contributor(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            type = ContributorType.AUTHOR,
            name = "Josh Smith",
            contact = listOf(ContactDetail(telecom = listOf(ContactPoint(value = "josh@projectronin.com"))))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contributor)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "type" : "author",
            |  "name" : "Josh Smith",
            |  "contact" : [ {
            |    "telecom" : [ {
            |      "value" : "josh@projectronin.com"
            |    } ]
            |  } ]
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedExtension = objectMapper.readValue<Contributor>(json)
        assertEquals(contributor, deserializedExtension)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val contributor = Contributor(
            type = ContributorType.AUTHOR,
            name = "Josh Smith"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contributor)

        val expectedJson = """
            |{
            |  "type" : "author",
            |  "name" : "Josh Smith"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "type" : "author",
            |  "name" : "Josh Smith"
            |}""".trimMargin()
        val contributor = objectMapper.readValue<Contributor>(json)

        assertNull(contributor.id)
        assertEquals(listOf<Extension>(), contributor.extension)
        assertEquals(ContributorType.AUTHOR, contributor.type)
        assertEquals("Josh Smith", contributor.name)
        assertEquals(listOf<ContactDetail>(), contributor.contact)
    }
}
