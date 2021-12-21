package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MetaTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val meta = Meta(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            versionId = Id("2"),
            lastUpdated = Instant("2017-01-01T00:00:00Z"),
            source = Uri("source-uri"),
            profile = listOf(Canonical("profile-canonical")),
            security = listOf(Coding(display = "security-coding")),
            tag = listOf(Coding(display = "tag-coding"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(meta)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "versionId" : "2",
            |  "lastUpdated" : "2017-01-01T00:00:00Z",
            |  "source" : "source-uri",
            |  "profile" : [ "profile-canonical" ],
            |  "security" : [ {
            |    "display" : "security-coding"
            |  } ],
            |  "tag" : [ {
            |    "display" : "tag-coding"
            |  } ]
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedMeta = objectMapper.readValue<Meta>(json)
        assertEquals(meta, deserializedMeta)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val meta = Meta(versionId = Id("2"))
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(meta)

        val expectedJson = """
            |{
            |  "versionId" : "2"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "source" : "source-uri"
            |}""".trimMargin()
        val meta = objectMapper.readValue<Meta>(json)

        assertNull(meta.id)
        assertEquals(listOf<Extension>(), meta.extension)
        assertNull(meta.versionId)
        assertNull(meta.lastUpdated)
        assertEquals(Uri("source-uri"), meta.source)
        assertEquals(listOf<Canonical>(), meta.profile)
        assertEquals(listOf<Coding>(), meta.security)
        assertEquals(listOf<Coding>(), meta.tag)
    }
}
