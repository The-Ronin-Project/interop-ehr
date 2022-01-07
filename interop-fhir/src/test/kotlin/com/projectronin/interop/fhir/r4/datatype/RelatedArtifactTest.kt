package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.Url
import com.projectronin.interop.fhir.r4.valueset.RelatedArtifactType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RelatedArtifactTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val relatedArtifact = RelatedArtifact(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            type = RelatedArtifactType.DOCUMENTATION,
            label = "artifact label",
            display = "Artifact",
            citation = Markdown("citation-markdown"),
            url = Url("http://localhost/artifact"),
            document = Attachment(url = Url("http://localhost/artifact/document")),
            resource = Canonical("resource-canonical")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(relatedArtifact)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "type" : "documentation",
            |  "label" : "artifact label",
            |  "display" : "Artifact",
            |  "citation" : "citation-markdown",
            |  "url" : "http://localhost/artifact",
            |  "document" : {
            |    "url" : "http://localhost/artifact/document"
            |  },
            |  "resource" : "resource-canonical"
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedRelatedArtifact = objectMapper.readValue<RelatedArtifact>(json)
        assertEquals(relatedArtifact, deserializedRelatedArtifact)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val relatedArtifact = RelatedArtifact(type = RelatedArtifactType.DOCUMENTATION)
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(relatedArtifact)

        val expectedJson = """
            |{
            |  "type" : "documentation"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "type" : "documentation"
            |}""".trimMargin()
        val relatedArtifact = objectMapper.readValue<RelatedArtifact>(json)

        assertNull(relatedArtifact.id)
        assertEquals(listOf<Extension>(), relatedArtifact.extension)
        assertEquals(RelatedArtifactType.DOCUMENTATION, relatedArtifact.type)
        assertNull(relatedArtifact.label)
        assertNull(relatedArtifact.display)
        assertNull(relatedArtifact.citation)
        assertNull(relatedArtifact.url)
        assertNull(relatedArtifact.document)
        assertNull(relatedArtifact.resource)
    }
}
