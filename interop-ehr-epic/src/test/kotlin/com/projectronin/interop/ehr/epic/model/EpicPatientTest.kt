package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.NameUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicPatientTest {
    @Test
    fun `can build from object`() {
        val identifier = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val name = HumanName(use = NameUse.USUAL)
        val telecom = ContactPoint(value = "123-456-7890")
        val address = Address(state = "MO")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier),
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1990-02-13"),
            telecom = listOf(telecom),
            address = listOf(address)
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(AdministrativeGender.MALE, epicPatient.gender)
        assertEquals("1990-02-13", epicPatient.birthDate)

        assertEquals(1, epicPatient.identifier.size)
        assertEquals(identifier, epicPatient.identifier[0].element)

        assertEquals(1, epicPatient.name.size)
        assertEquals(name, epicPatient.name[0].element)

        assertEquals(1, epicPatient.telecom.size)
        assertEquals(telecom, epicPatient.telecom[0].element)

        assertEquals(1, epicPatient.address.size)
        assertEquals(address, epicPatient.address[0].element)
    }

    @Test
    fun `supports no identifier`() {
        val name = HumanName(use = NameUse.USUAL)
        val telecom = ContactPoint(value = "123-456-7890")
        val address = Address(state = "MO")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(),
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1990-02-13"),
            telecom = listOf(telecom),
            address = listOf(address)
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(0, epicPatient.identifier.size)
        assertEquals(1, epicPatient.name.size)
        assertEquals(name, epicPatient.name[0].element)
        assertEquals(AdministrativeGender.MALE, epicPatient.gender)
        assertEquals("1990-02-13", epicPatient.birthDate)
        assertEquals(1, epicPatient.telecom.size)
        assertEquals(telecom, epicPatient.telecom[0].element)
        assertEquals(1, epicPatient.address.size)
        assertEquals(address, epicPatient.address[0].element)
    }

    @Test
    fun `supports multiple identifiers`() {
        val identifier1 = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val identifier2 = Identifier(system = Uri("PID"), value = "98765")
        val name = HumanName(use = NameUse.USUAL)
        val telecom = ContactPoint(value = "123-456-7890")
        val address = Address(state = "MO")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier1, identifier2),
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1990-02-13"),
            telecom = listOf(telecom),
            address = listOf(address)
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(2, epicPatient.identifier.size)
        assertEquals(identifier1, epicPatient.identifier[0].element)
        assertEquals(identifier2, epicPatient.identifier[1].element)
        assertEquals(1, epicPatient.name.size)
        assertEquals(name, epicPatient.name[0].element)
        assertEquals(AdministrativeGender.MALE, epicPatient.gender)
        assertEquals("1990-02-13", epicPatient.birthDate)
        assertEquals(1, epicPatient.telecom.size)
        assertEquals(telecom, epicPatient.telecom[0].element)
        assertEquals(1, epicPatient.address.size)
        assertEquals(address, epicPatient.address[0].element)
    }

    @Test
    fun `supports no name`() {
        val identifier = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val telecom = ContactPoint(value = "123-456-7890")
        val address = Address(state = "MO")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier),
            name = listOf(),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1990-02-13"),
            telecom = listOf(telecom),
            address = listOf(address)
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(1, epicPatient.identifier.size)
        assertEquals(identifier, epicPatient.identifier[0].element)
        assertEquals(0, epicPatient.name.size)
        assertEquals(AdministrativeGender.MALE, epicPatient.gender)
        assertEquals("1990-02-13", epicPatient.birthDate)
        assertEquals(1, epicPatient.telecom.size)
        assertEquals(telecom, epicPatient.telecom[0].element)
        assertEquals(1, epicPatient.address.size)
        assertEquals(address, epicPatient.address[0].element)
    }

    @Test
    fun `supports multiple names`() {
        val identifier = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val name1 = HumanName(use = NameUse.USUAL)
        val name2 = HumanName(use = NameUse.MAIDEN)
        val telecom = ContactPoint(value = "123-456-7890")
        val address = Address(state = "MO")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier),
            name = listOf(name1, name2),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1990-02-13"),
            telecom = listOf(telecom),
            address = listOf(address)
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(1, epicPatient.identifier.size)
        assertEquals(identifier, epicPatient.identifier[0].element)
        assertEquals(2, epicPatient.name.size)
        assertEquals(name1, epicPatient.name[0].element)
        assertEquals(name2, epicPatient.name[1].element)
        assertEquals(AdministrativeGender.MALE, epicPatient.gender)
        assertEquals("1990-02-13", epicPatient.birthDate)
        assertEquals(1, epicPatient.telecom.size)
        assertEquals(telecom, epicPatient.telecom[0].element)
        assertEquals(1, epicPatient.address.size)
        assertEquals(address, epicPatient.address[0].element)
    }

    @Test
    fun `supports no gender`() {
        val identifier = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val name = HumanName(use = NameUse.USUAL)
        val telecom = ContactPoint(value = "123-456-7890")
        val address = Address(state = "MO")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier),
            name = listOf(name),
            birthDate = Date("1990-02-13"),
            telecom = listOf(telecom),
            address = listOf(address)
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(1, epicPatient.identifier.size)
        assertEquals(identifier, epicPatient.identifier[0].element)
        assertEquals(1, epicPatient.name.size)
        assertEquals(name, epicPatient.name[0].element)
        assertNull(epicPatient.gender)
        assertEquals("1990-02-13", epicPatient.birthDate)
        assertEquals(1, epicPatient.telecom.size)
        assertEquals(telecom, epicPatient.telecom[0].element)
        assertEquals(1, epicPatient.address.size)
        assertEquals(address, epicPatient.address[0].element)
    }

    @Test
    fun `supports no birth date`() {
        val identifier = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val name = HumanName(use = NameUse.USUAL)
        val telecom = ContactPoint(value = "123-456-7890")
        val address = Address(state = "MO")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier),
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            telecom = listOf(telecom),
            address = listOf(address)
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(1, epicPatient.identifier.size)
        assertEquals(identifier, epicPatient.identifier[0].element)
        assertEquals(1, epicPatient.name.size)
        assertEquals(name, epicPatient.name[0].element)
        assertEquals(AdministrativeGender.MALE, epicPatient.gender)
        assertNull(epicPatient.birthDate)
        assertEquals(1, epicPatient.telecom.size)
        assertEquals(telecom, epicPatient.telecom[0].element)
        assertEquals(1, epicPatient.address.size)
        assertEquals(address, epicPatient.address[0].element)
    }

    @Test
    fun `supports no telecom`() {
        val identifier = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val name = HumanName(use = NameUse.USUAL)
        val address = Address(state = "MO")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier),
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1990-02-13"),
            telecom = listOf(),
            address = listOf(address)
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(1, epicPatient.identifier.size)
        assertEquals(identifier, epicPatient.identifier[0].element)
        assertEquals(1, epicPatient.name.size)
        assertEquals(name, epicPatient.name[0].element)
        assertEquals(AdministrativeGender.MALE, epicPatient.gender)
        assertEquals("1990-02-13", epicPatient.birthDate)
        assertEquals(0, epicPatient.telecom.size)
        assertEquals(1, epicPatient.address.size)
        assertEquals(address, epicPatient.address[0].element)
    }

    @Test
    fun `supports multiple telecoms`() {
        val identifier = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val name = HumanName(use = NameUse.USUAL)
        val telecom1 = ContactPoint(value = "123-456-7890")
        val telecom2 = ContactPoint(value = "test@projectronin.com")
        val address = Address(state = "MO")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier),
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1990-02-13"),
            telecom = listOf(telecom1, telecom2),
            address = listOf(address)
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(1, epicPatient.identifier.size)
        assertEquals(identifier, epicPatient.identifier[0].element)
        assertEquals(1, epicPatient.name.size)
        assertEquals(name, epicPatient.name[0].element)
        assertEquals(AdministrativeGender.MALE, epicPatient.gender)
        assertEquals("1990-02-13", epicPatient.birthDate)
        assertEquals(2, epicPatient.telecom.size)
        assertEquals(telecom1, epicPatient.telecom[0].element)
        assertEquals(telecom2, epicPatient.telecom[1].element)
        assertEquals(1, epicPatient.address.size)
        assertEquals(address, epicPatient.address[0].element)
    }

    @Test
    fun `supports no address`() {
        val identifier = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val name = HumanName(use = NameUse.USUAL)
        val telecom = ContactPoint(value = "123-456-7890")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier),
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1990-02-13"),
            telecom = listOf(telecom),
            address = listOf()
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(1, epicPatient.identifier.size)
        assertEquals(identifier, epicPatient.identifier[0].element)
        assertEquals(1, epicPatient.name.size)
        assertEquals(name, epicPatient.name[0].element)
        assertEquals(AdministrativeGender.MALE, epicPatient.gender)
        assertEquals("1990-02-13", epicPatient.birthDate)
        assertEquals(1, epicPatient.telecom.size)
        assertEquals(telecom, epicPatient.telecom[0].element)
        assertEquals(0, epicPatient.address.size)
    }

    @Test
    fun `supports multiple addresses`() {
        val identifier = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val name = HumanName(use = NameUse.USUAL)
        val telecom = ContactPoint(value = "123-456-7890")
        val address1 = Address(state = "MO")
        val address2 = Address(state = "GA")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier),
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1990-02-13"),
            telecom = listOf(telecom),
            address = listOf(address1, address2)
        )

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(DataSource.FHIR_R4, epicPatient.dataSource)
        assertEquals(ResourceType.PATIENT, epicPatient.resourceType)
        assertEquals("1234567890", epicPatient.id)
        assertEquals(1, epicPatient.identifier.size)
        assertEquals(identifier, epicPatient.identifier[0].element)
        assertEquals(1, epicPatient.name.size)
        assertEquals(name, epicPatient.name[0].element)
        assertEquals(AdministrativeGender.MALE, epicPatient.gender)
        assertEquals("1990-02-13", epicPatient.birthDate)
        assertEquals(1, epicPatient.telecom.size)
        assertEquals(telecom, epicPatient.telecom[0].element)
        assertEquals(2, epicPatient.address.size)
        assertEquals(address1, epicPatient.address[0].element)
        assertEquals(address2, epicPatient.address[1].element)
    }

    @Test
    fun `returns JSON as raw`() {
        val identifier = Identifier(system = Uri("SSN"), value = "123-45-6789")
        val name = HumanName(use = NameUse.USUAL)
        val telecom = ContactPoint(value = "123-456-7890")
        val address = Address(state = "MO")
        val patient = Patient(
            id = Id("1234567890"),
            identifier = listOf(identifier),
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1990-02-13"),
            telecom = listOf(telecom),
            address = listOf(address)
        )

        val identifierJson = """{"system":"SSN","value":"123-45-6789"}"""
        val nameJson = """{"use":"usual"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val addressJson = """{"state":"MO"}"""

        val json = """{
            |   "resourceType" : "Patient",
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson
            |   ],
            |   "name": [
            |      $nameJson
            |   ],
            |   "telecom": [
            |      $telecomJson
            |   ],
            |   "gender": "male",
            |   "birthDate": "1990-02-13",
            |   "address": [
            |      $addressJson
            |   ]
            |}""".trimMargin()

        val epicPatient = EpicPatient(patient)
        assertEquals(patient, epicPatient.resource)
        assertEquals(deformat(json), epicPatient.raw)

        assertEquals(1, epicPatient.identifier.size)
        assertEquals(identifier, epicPatient.identifier[0].element)
        assertEquals(identifierJson, epicPatient.identifier[0].raw)

        assertEquals(1, epicPatient.name.size)
        assertEquals(name, epicPatient.name[0].element)
        assertEquals(nameJson, epicPatient.name[0].raw)

        assertEquals(1, epicPatient.telecom.size)
        assertEquals(telecom, epicPatient.telecom[0].element)
        assertEquals(telecomJson, epicPatient.telecom[0].raw)

        assertEquals(1, epicPatient.address.size)
        assertEquals(address, epicPatient.address[0].element)
        assertEquals(addressJson, epicPatient.address[0].raw)
    }
}
