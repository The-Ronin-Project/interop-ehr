package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class QualificationTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val qualification = Qualification(
            id = "67890",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id")),
            code = CodeableConcept(text = "code"),
            period = Period(start = DateTime("2001")),
            issuer = Reference(reference = "Organization/12345")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(qualification)

        val expectedJson = """
            |{
            |  "id" : "67890",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "modifierExtension" : [ {
            |    "url" : "http://localhost/modifier-extension",
            |    "valueString" : "Value"
            |  } ],
            |  "identifier" : [ {
            |    "value" : "id"
            |  } ],
            |  "code" : {
            |    "text" : "code"
            |  },
            |  "period" : {
            |    "start" : "2001"
            |  },
            |  "issuer" : {
            |    "reference" : "Organization/12345"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedQualification = objectMapper.readValue<Qualification>(json)
        assertEquals(qualification, deserializedQualification)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val qualification = Qualification(
            code = CodeableConcept(text = "code")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(qualification)

        val expectedJson = """
            |{
            |  "code" : {
            |    "text" : "code"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "code" : {
            |    "text" : "code"
            |  }
            |}""".trimMargin()
        val qualification = objectMapper.readValue<Qualification>(json)

        assertNull(qualification.id)
        assertEquals(listOf<Extension>(), qualification.extension)
        assertEquals(listOf<Extension>(), qualification.modifierExtension)
        assertEquals(listOf<Identifier>(), qualification.identifier)
        assertEquals(CodeableConcept(text = "code"), qualification.code)
        assertNull(qualification.period)
        assertNull(qualification.issuer)
    }
}
