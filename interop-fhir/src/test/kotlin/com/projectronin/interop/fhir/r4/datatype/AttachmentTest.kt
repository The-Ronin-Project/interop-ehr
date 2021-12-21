package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.Url
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AttachmentTest {
    @Test
    fun `fails if data provided without content type`() {
        val exception = assertThrows<IllegalArgumentException> { Attachment(data = Base64Binary("abcd")) }
        assertEquals("If the Attachment has data, it SHALL have a contentType", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val attachment = Attachment(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            contentType = Code("application/json"),
            language = Code("en-US"),
            data = Base64Binary("abcd"),
            url = Url("http://localhost/data"),
            size = UnsignedInt(300),
            hash = Base64Binary("1234"),
            title = "Title",
            creation = DateTime("2021-11-17T17:39:00Z")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(attachment)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "contentType" : "application/json",
            |  "language" : "en-US",
            |  "data" : "abcd",
            |  "url" : "http://localhost/data",
            |  "size" : 300,
            |  "hash" : "1234",
            |  "title" : "Title",
            |  "creation" : "2021-11-17T17:39:00Z"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedAttachment = objectMapper.readValue<Attachment>(json)
        assertEquals(attachment, deserializedAttachment)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val attachment = Attachment(
            title = "Empty"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(attachment)

        val expectedJson = """
            |{
            |  "title" : "Empty"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "url" : "http://localhost/data"
            |}""".trimMargin()
        val attachment = objectMapper.readValue<Attachment>(json)

        assertNull(attachment.id)
        assertEquals(listOf<Extension>(), attachment.extension)
        assertNull(attachment.contentType)
        assertNull(attachment.language)
        assertNull(attachment.data)
        assertEquals(Url("http://localhost/data"), attachment.url)
        assertNull(attachment.size)
        assertNull(attachment.hash)
        assertNull(attachment.title)
        assertNull(attachment.creation)
    }
}
