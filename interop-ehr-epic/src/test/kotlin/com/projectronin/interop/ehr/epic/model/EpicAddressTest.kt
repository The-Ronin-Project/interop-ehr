package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.valueset.AddressUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicAddressTest {
    @Test
    fun `can build from object`() {
        val address = Address(
            use = AddressUse.HOME,
            line = listOf("123 Main St."),
            city = "Madison",
            district = "DANE",
            state = "WI",
            postalCode = "53703",
            country = "US"
        )

        val epicAddress = EpicAddress(address)
        assertEquals(address, epicAddress.element)
        assertEquals(AddressUse.HOME, epicAddress.use)
        assertEquals(listOf("123 Main St."), epicAddress.line)
        assertEquals("Madison", epicAddress.city)
        assertEquals("WI", epicAddress.state)
        assertEquals("53703", epicAddress.postalCode)
    }

    @Test
    fun `supports no use`() {
        val address = Address(
            line = listOf("123 Main St."),
            city = "Madison",
            district = "DANE",
            state = "WI",
            postalCode = "53703",
            country = "US"
        )

        val epicAddress = EpicAddress(address)
        assertEquals(address, epicAddress.element)
        assertNull(epicAddress.use)
        assertEquals(listOf("123 Main St."), epicAddress.line)
        assertEquals("Madison", epicAddress.city)
        assertEquals("WI", epicAddress.state)
        assertEquals("53703", epicAddress.postalCode)
    }

    @Test
    fun `supports no line`() {
        val address = Address(
            use = AddressUse.HOME,
            city = "Madison",
            district = "DANE",
            state = "WI",
            postalCode = "53703",
            country = "US"
        )

        val epicAddress = EpicAddress(address)
        assertEquals(address, epicAddress.element)
        assertEquals(AddressUse.HOME, epicAddress.use)
        assertEquals(listOf<String>(), epicAddress.line)
        assertEquals("Madison", epicAddress.city)
        assertEquals("WI", epicAddress.state)
        assertEquals("53703", epicAddress.postalCode)
    }

    @Test
    fun `supports multiple lines`() {
        val address = Address(
            use = AddressUse.HOME,
            line = listOf("123 Main St.", "Apt 456"),
            city = "Madison",
            district = "DANE",
            state = "WI",
            postalCode = "53703",
            country = "US"
        )

        val epicAddress = EpicAddress(address)
        assertEquals(address, epicAddress.element)
        assertEquals(AddressUse.HOME, epicAddress.use)
        assertEquals(listOf("123 Main St.", "Apt 456"), epicAddress.line)
        assertEquals("Madison", epicAddress.city)
        assertEquals("WI", epicAddress.state)
        assertEquals("53703", epicAddress.postalCode)
    }

    @Test
    fun `supports no city`() {
        val address = Address(
            use = AddressUse.HOME,
            line = listOf("123 Main St."),
            district = "DANE",
            state = "WI",
            postalCode = "53703",
            country = "US"
        )

        val epicAddress = EpicAddress(address)
        assertEquals(address, epicAddress.element)
        assertEquals(AddressUse.HOME, epicAddress.use)
        assertEquals(listOf("123 Main St."), epicAddress.line)
        assertNull(epicAddress.city)
        assertEquals("WI", epicAddress.state)
        assertEquals("53703", epicAddress.postalCode)
    }

    @Test
    fun `supports no state`() {
        val address = Address(
            use = AddressUse.HOME,
            line = listOf("123 Main St."),
            city = "Madison",
            district = "DANE",
            postalCode = "53703",
            country = "US"
        )

        val epicAddress = EpicAddress(address)
        assertEquals(address, epicAddress.element)
        assertEquals(AddressUse.HOME, epicAddress.use)
        assertEquals(listOf("123 Main St."), epicAddress.line)
        assertEquals("Madison", epicAddress.city)
        assertNull(epicAddress.state)
        assertEquals("53703", epicAddress.postalCode)
    }

    @Test
    fun `supports no postal code`() {
        val address = Address(
            use = AddressUse.HOME,
            line = listOf("123 Main St."),
            city = "Madison",
            district = "DANE",
            state = "WI",
            country = "US"
        )

        val epicAddress = EpicAddress(address)
        assertEquals(address, epicAddress.element)
        assertEquals(AddressUse.HOME, epicAddress.use)
        assertEquals(listOf("123 Main St."), epicAddress.line)
        assertEquals("Madison", epicAddress.city)
        assertEquals("WI", epicAddress.state)
        assertNull(epicAddress.postalCode)
    }

    @Test
    fun `returns JSON as raw`() {
        val address = Address(
            use = AddressUse.HOME,
            line = listOf("123 Main St."),
            city = "Madison",
            district = "DANE",
            state = "WI",
            postalCode = "53703",
            country = "US"
        )
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

        val epicAddress = EpicAddress(address)
        assertEquals(address, epicAddress.element)
        assertEquals(deformat(json), epicAddress.raw)
    }
}
