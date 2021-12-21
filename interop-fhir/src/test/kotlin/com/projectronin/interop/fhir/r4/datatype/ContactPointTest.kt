package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ContactPointTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val contactPoint = ContactPoint(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            system = ContactPointSystem.EMAIL,
            value = "name@site.com",
            use = ContactPointUse.HOME,
            rank = PositiveInt(1),
            period = Period(start = DateTime("2021-11-18"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contactPoint)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "system" : "email",
            |  "value" : "name@site.com",
            |  "use" : "home",
            |  "rank" : 1,
            |  "period" : {
            |    "start" : "2021-11-18"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedContactPoint = objectMapper.readValue<ContactPoint>(json)
        assertEquals(contactPoint, deserializedContactPoint)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val contactPoint = ContactPoint(
            value = "name@site.com"
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contactPoint)

        val expectedJson = """
            |{
            |  "value" : "name@site.com"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "period" : {
            |    "start" : "2021-11-18"
            |  }
            |}""".trimMargin()
        val contactPoint = objectMapper.readValue<ContactPoint>(json)

        assertNull(contactPoint.id)
        assertEquals(listOf<Extension>(), contactPoint.extension)
        assertNull(contactPoint.system)
        assertNull(contactPoint.value)
        assertNull(contactPoint.use)
        assertNull(contactPoint.rank)
        assertEquals(Period(start = DateTime("2021-11-18")), contactPoint.period)
    }
}
