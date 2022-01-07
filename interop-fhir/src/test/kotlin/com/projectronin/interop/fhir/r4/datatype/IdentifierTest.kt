package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IdentifierTest {
    @Test
    fun `fails if assigner supplied type is not Organization`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Identifier(assigner = Reference(type = Uri("Patient")))
            }
        assertEquals("Assigner must belong to an Organization", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val identifier = Identifier(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            use = IdentifierUse.OFFICIAL,
            type = CodeableConcept(text = "concept"),
            system = Uri("identifier-system"),
            value = "identifier value",
            period = Period(start = DateTime("2021-11-01")),
            assigner = Reference(reference = "Organization")
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(identifier)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "use" : "official",
            |  "type" : {
            |    "text" : "concept"
            |  },
            |  "system" : "identifier-system",
            |  "value" : "identifier value",
            |  "period" : {
            |    "start" : "2021-11-01"
            |  },
            |  "assigner" : {
            |    "reference" : "Organization"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedIdentifier = JacksonManager.objectMapper.readValue<Identifier>(json)
        assertEquals(identifier, deserializedIdentifier)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val identifier = Identifier(use = IdentifierUse.OFFICIAL)
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(identifier)

        val expectedJson = """
            |{
            |  "use" : "official"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "value" : "identifier"
            |}""".trimMargin()
        val identifier = JacksonManager.objectMapper.readValue<Identifier>(json)

        assertNull(identifier.id)
        assertEquals(listOf<Extension>(), identifier.extension)
        assertNull(identifier.use)
        assertNull(identifier.type)
        assertNull(identifier.system)
        assertEquals("identifier", identifier.value)
        assertNull(identifier.period)
        assertNull(identifier.assigner)
    }
}
