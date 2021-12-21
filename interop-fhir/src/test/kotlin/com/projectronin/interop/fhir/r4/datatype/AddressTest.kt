package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.fhir.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.AddressType
import com.projectronin.interop.fhir.r4.valueset.AddressUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AddressTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val address = Address(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            use = AddressUse.HOME,
            type = AddressType.POSTAL,
            text = "925 Powder Springs St, Smyrna, GA 30080",
            line = listOf("925 Powder Springs St"),
            city = "Smyrna",
            district = "Cobb",
            state = "GA",
            postalCode = "30080",
            country = "USA",
            period = Period(start = DateTime("1998-08"), end = DateTime("2002-05"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(address)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "use" : "home",
            |  "type" : "postal",
            |  "text" : "925 Powder Springs St, Smyrna, GA 30080",
            |  "line" : [ "925 Powder Springs St" ],
            |  "city" : "Smyrna",
            |  "district" : "Cobb",
            |  "state" : "GA",
            |  "postalCode" : "30080",
            |  "country" : "USA",
            |  "period" : {
            |    "start" : "1998-08",
            |    "end" : "2002-05"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedAddress = objectMapper.readValue<Address>(json)
        assertEquals(address, deserializedAddress)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val address = Address(type = AddressType.POSTAL)

        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(address)

        val expectedJson = """
            |{
            |  "type" : "postal"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "use" : "home"
            |}""".trimMargin()
        val address = objectMapper.readValue<Address>(json)

        assertNull(address.id)
        assertEquals(listOf<Extension>(), address.extension)
        assertEquals(AddressUse.HOME, address.use)
        assertNull(address.type)
        assertNull(address.text)
        assertEquals(listOf<String>(), address.line)
        assertNull(address.city)
        assertNull(address.district)
        assertNull(address.state)
        assertNull(address.postalCode)
        assertNull(address.country)
        assertNull(address.period)
    }
}
