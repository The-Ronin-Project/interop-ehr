package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import org.hl7.cql.model.Relationship
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ContactTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val contact = Contact(
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
            relationship = listOf(
                CodeableConcept(text = "N")
            ),
            name = HumanName(text = "Jane Doe"),
            telecom = listOf(ContactPoint(value = "name@site.com")),
            address = Address(text = "123 Sesame St"),
            gender = AdministrativeGender.MALE,
            organization = Reference(reference = "Patient/123"),
            period = Period(start = DateTime("1998-08"))
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contact)

        val expectedJson = """
            {
              "id" : "67890",
              "extension" : [ {
                "url" : "http://localhost/extension",
                "valueString" : "Value"
              } ],
              "modifierExtension" : [ {
                "url" : "http://localhost/modifier-extension",
                "valueString" : "Value"
              } ],
              "relationship" : [ {
                "text" : "N"
              } ],
              "name" : {
                "text" : "Jane Doe"
              },
              "telecom" : [ {
                "value" : "name@site.com"
              } ],
              "address" : {
                "text" : "123 Sesame St"
              },
              "gender" : "male",
              "organization" : {
                "reference" : "Patient/123"
              },
              "period" : {
                "start" : "1998-08"
              }
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedContact = JacksonManager.objectMapper.readValue<Contact>(json)
        assertEquals(contact, deserializedContact)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val contact = Contact()
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contact)

        val expectedJson = "{ }"
        assertEquals(expectedJson, json)

        val deserializedContact = JacksonManager.objectMapper.readValue<Contact>(json)
        assertEquals(contact, deserializedContact)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
                "organization" : {
                    "reference" : "Patient/123"
                }
            }
        """.trimIndent()
        val contact = JacksonManager.objectMapper.readValue<Contact>(json)

        assertNull(contact.id)
        assertEquals(listOf<Extension>(), contact.extension)
        assertEquals(listOf<Extension>(), contact.modifierExtension)
        assertEquals(listOf<Relationship>(), contact.relationship)
        assertNull(contact.name)
        assertEquals(listOf<ContactPoint>(), contact.telecom)
        assertNull(contact.address)
        assertNull(contact.gender)
        assertNull(contact.period)
    }
}
