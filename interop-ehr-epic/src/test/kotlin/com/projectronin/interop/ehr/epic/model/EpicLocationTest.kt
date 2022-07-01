package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicLocationTest {
    @Test
    fun `can build from object`() {
        val id = Id("e4aPTZoZLqOja.QwzaEzp0A3")
        val identifier = Identifier(system = Uri("abc123"), type = CodeableConcept(text = "Internal"), value = "E14345")
        val telecom = ContactPoint(system = ContactPointSystem.PHONE, value = "123-456-7890")
        val address = Address(line = listOf("1 Maple Drive", "Apt A"), city = "Anytown", state = "MD", postalCode = "12345")
        val location = Location(id = id, name = "My Office", identifier = listOf(identifier), telecom = listOf(telecom), address = address)
        val epicLocation = EpicLocation(location)
        assertEquals(location, epicLocation.resource)

        // Id, Name
        assertEquals("e4aPTZoZLqOja.QwzaEzp0A3", epicLocation.id)
        assertEquals("My Office", epicLocation.name)

        // Identifier
        assertEquals(1, epicLocation.identifier.size)
        assertEquals(identifier, epicLocation.identifier[0].element)
        assertEquals("Internal", epicLocation.identifier[0].type?.text)
        assertEquals("abc123", epicLocation.identifier[0].system)
        assertEquals("E14345", epicLocation.identifier[0].value)

        // Telecom
        assertEquals(1, epicLocation.telecom.size)
        assertEquals(telecom, epicLocation.telecom[0].element)
        assertEquals("123-456-7890", epicLocation.telecom[0].value)

        // Address
        assertEquals("Apt A", epicLocation.address!!.line[1])
        assertEquals("MD", epicLocation.address?.state)
    }

    @Test
    fun `can build from null and missing values`() {
        val id = Id("e4aPTZoZLqOja.QwzaEzp0A3")
        val address = Address(city = "Anytown", state = "MD")
        val location = Location(id = id, identifier = listOf(), telecom = listOf(), address = address)
        val epicLocation = EpicLocation(location)
        assertEquals(location, epicLocation.resource)

        assertEquals(DataSource.FHIR_R4, epicLocation.dataSource)
        assertEquals(ResourceType.LOCATION, epicLocation.resourceType)
        assertEquals("e4aPTZoZLqOja.QwzaEzp0A3", epicLocation.id)
        assertNull(epicLocation.name)
        assertEquals(0, epicLocation.identifier.size)
        assertEquals(0, epicLocation.telecom.size)
        assertEquals("Anytown", epicLocation.address?.city)
    }

    @Test
    fun `return JSON as raw`() {
        val id = Id("e4aPTZoZLqOja.QwzaEzp0A3")
        val identifier = Identifier(system = Uri("abc123"), type = CodeableConcept(text = "Internal"), value = "E14345")
        val telecom = ContactPoint(system = ContactPointSystem.PHONE, value = "123-456-7890")
        val address = Address(line = listOf("1 Maple Drive", "Apt A"), city = "Anytown", state = "MD", postalCode = "12345")
        val location = Location(id = id, name = "My Office", identifier = listOf(identifier), telecom = listOf(telecom), address = address)
        val epicLocation = EpicLocation(location)
        assertEquals(location, epicLocation.resource)

        val identifierJSON = """{"type":{"text":"Internal"},"system":"abc123","value":"E14345"}"""
        val telecomJson = """{"system":"phone","value":"123-456-7890"}"""
        val addressJSON = """{"line":["1 Maple Drive","Apt A"],"city":"Anytown","state":"MD","postalCode":"12345"}"""
        val json = """{
            |"resourceType": "Location",
            |"id": "e4aPTZoZLqOja.QwzaEzp0A3",
            |"identifier": [$identifierJSON],
            |"name": "My Office",
            |"telecom": [$telecomJson],
            |"address": $addressJSON
            |}""".trimMargin()
        assertEquals(deformat(json), epicLocation.raw)

        // Identifier
        assertEquals(1, epicLocation.identifier.size)
        assertEquals(identifier, epicLocation.identifier[0].element)
        assertEquals(identifierJSON, epicLocation.identifier[0].raw)

        // Telecom
        assertEquals(1, epicLocation.telecom.size)
        assertEquals(telecom, epicLocation.telecom[0].element)
        assertEquals(telecomJson, epicLocation.telecom[0].raw)
    }
}
