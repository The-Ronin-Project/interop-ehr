package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.fhir.r4.valueset.AddressUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicAddressTest {
    @Test
    fun `can build from JSON`() {
        val json = """{
          |  "use": "home",
          |  "line": [
          |    "123 Main St."
          |  ],
          |  "city": "Madison",
          |  "district": "DANE",
          |  "state": "WI",
          |  "postalCode": "53703",
          |  "country": "US"
          |}""".trimMargin()

        val address = EpicAddress(json)
        assertEquals(json, address.raw)
        assertEquals(AddressUse.HOME, address.use)
        assertEquals(listOf("123 Main St."), address.line)
        assertEquals("Madison", address.city)
        assertEquals("WI", address.state)
        assertEquals("53703", address.postalCode)
    }

    @Test
    fun `supports no use`() {
        val json = """{
          |  "line": [
          |    "123 Main St."
          |  ],
          |  "city": "Madison",
          |  "district": "DANE",
          |  "state": "WI",
          |  "postalCode": "53703",
          |  "country": "US"
          |}""".trimMargin()

        val address = EpicAddress(json)
        assertEquals(json, address.raw)
        assertNull(address.use)
        assertEquals(listOf("123 Main St."), address.line)
        assertEquals("Madison", address.city)
        assertEquals("WI", address.state)
        assertEquals("53703", address.postalCode)
    }

    @Test
    fun `supports no line`() {
        val json = """{
          |  "use": "home",
          |  "city": "Madison",
          |  "district": "DANE",
          |  "state": "WI",
          |  "postalCode": "53703",
          |  "country": "US"
          |}""".trimMargin()

        val address = EpicAddress(json)
        assertEquals(json, address.raw)
        assertEquals(AddressUse.HOME, address.use)
        assertEquals(listOf<String>(), address.line)
        assertEquals("Madison", address.city)
        assertEquals("WI", address.state)
        assertEquals("53703", address.postalCode)
    }

    @Test
    fun `supports multiple lines`() {
        val json = """{
          |  "use": "home",
          |  "line": [
          |    "123 Main St.",
          |    "Apt 456"
          |  ],
          |  "city": "Madison",
          |  "district": "DANE",
          |  "state": "WI",
          |  "postalCode": "53703",
          |  "country": "US"
          |}""".trimMargin()

        val address = EpicAddress(json)
        assertEquals(json, address.raw)
        assertEquals(AddressUse.HOME, address.use)
        assertEquals(listOf("123 Main St.", "Apt 456"), address.line)
        assertEquals("Madison", address.city)
        assertEquals("WI", address.state)
        assertEquals("53703", address.postalCode)
    }

    @Test
    fun `supports no city`() {
        val json = """{
          |  "use": "home",
          |  "line": [
          |    "123 Main St."
          |  ],
          |  "district": "DANE",
          |  "state": "WI",
          |  "postalCode": "53703",
          |  "country": "US"
          |}""".trimMargin()

        val address = EpicAddress(json)
        assertEquals(json, address.raw)
        assertEquals(AddressUse.HOME, address.use)
        assertEquals(listOf("123 Main St."), address.line)
        assertNull(address.city)
        assertEquals("WI", address.state)
        assertEquals("53703", address.postalCode)
    }

    @Test
    fun `supports no state`() {
        val json = """{
          |  "use": "home",
          |  "line": [
          |    "123 Main St."
          |  ],
          |  "city": "Madison",
          |  "district": "DANE",
          |  "postalCode": "53703",
          |  "country": "US"
          |}""".trimMargin()

        val address = EpicAddress(json)
        assertEquals(json, address.raw)
        assertEquals(AddressUse.HOME, address.use)
        assertEquals(listOf("123 Main St."), address.line)
        assertEquals("Madison", address.city)
        assertNull(address.state)
        assertEquals("53703", address.postalCode)
    }

    @Test
    fun `supports no postal code`() {
        val json = """{
          |  "use": "home",
          |  "line": [
          |    "123 Main St."
          |  ],
          |  "city": "Madison",
          |  "district": "DANE",
          |  "state": "WI",
          |  "country": "US"
          |}""".trimMargin()

        val address = EpicAddress(json)
        assertEquals(json, address.raw)
        assertEquals(AddressUse.HOME, address.use)
        assertEquals(listOf("123 Main St."), address.line)
        assertEquals("Madison", address.city)
        assertEquals("WI", address.state)
        assertNull(address.postalCode)
    }
}
