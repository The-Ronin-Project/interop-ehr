package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicPatientTest {
    @Test
    fun `can build from JSON`() {
        val identifierJson = """{"system":"SSN","value":"123-45-6789"}"""
        val nameJson = """{"use":"usual"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val addressJson = """{"state":"MO"}"""

        val json = """{
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson
            |   ],
            |   "name": [
            |      $nameJson
            |   ],
            |   "gender": "male",
            |   "birthDate": "1990-02-13",
            |   "telecom": [
            |      $telecomJson
            |   ],
            |   "address": [
            |      $addressJson
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(1, patient.identifier.size)
        assertEquals(identifierJson, patient.identifier[0].raw)
        assertEquals(1, patient.name.size)
        assertEquals(nameJson, patient.name[0].raw)
        assertEquals(AdministrativeGender.MALE, patient.gender)
        assertEquals("1990-02-13", patient.birthDate)
        assertEquals(1, patient.telecom.size)
        assertEquals(telecomJson, patient.telecom[0].raw)
        assertEquals(1, patient.address.size)
        assertEquals(addressJson, patient.address[0].raw)
    }

    @Test
    fun `supports no identifier`() {
        val nameJson = """{"use":"usual"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val addressJson = """{"state":"MO"}"""

        val json = """{
            |   "id": "1234567890",
            |   "name": [
            |      $nameJson
            |   ],
            |   "gender": "male",
            |   "birthDate": "1990-02-13",
            |   "telecom": [
            |      $telecomJson
            |   ],
            |   "address": [
            |      $addressJson
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(0, patient.identifier.size)
        assertEquals(1, patient.name.size)
        assertEquals(nameJson, patient.name[0].raw)
        assertEquals(AdministrativeGender.MALE, patient.gender)
        assertEquals("1990-02-13", patient.birthDate)
        assertEquals(1, patient.telecom.size)
        assertEquals(telecomJson, patient.telecom[0].raw)
        assertEquals(1, patient.address.size)
        assertEquals(addressJson, patient.address[0].raw)
    }

    @Test
    fun `supports multiple identifiers`() {
        val identifierJson1 = """{"system":"SSN","value":"123-45-6789"}"""
        val identifierJson2 = """{"system":"PID","value":"98765"}"""
        val nameJson = """{"use":"usual"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val addressJson = """{"state":"MO"}"""

        val json = """{
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson1,
            |      $identifierJson2
            |   ],
            |   "name": [
            |      $nameJson
            |   ],
            |   "gender": "male",
            |   "birthDate": "1990-02-13",
            |   "telecom": [
            |      $telecomJson
            |   ],
            |   "address": [
            |      $addressJson
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(2, patient.identifier.size)
        assertEquals(identifierJson1, patient.identifier[0].raw)
        assertEquals(identifierJson2, patient.identifier[1].raw)
        assertEquals(1, patient.name.size)
        assertEquals(nameJson, patient.name[0].raw)
        assertEquals(AdministrativeGender.MALE, patient.gender)
        assertEquals("1990-02-13", patient.birthDate)
        assertEquals(1, patient.telecom.size)
        assertEquals(telecomJson, patient.telecom[0].raw)
        assertEquals(1, patient.address.size)
        assertEquals(addressJson, patient.address[0].raw)
    }

    @Test
    fun `supports no name`() {
        val identifierJson = """{"system":"SSN","value":"123-45-6789"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val addressJson = """{"state":"MO"}"""

        val json = """{
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson
            |   ],
            |   "gender": "male",
            |   "birthDate": "1990-02-13",
            |   "telecom": [
            |      $telecomJson
            |   ],
            |   "address": [
            |      $addressJson
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(1, patient.identifier.size)
        assertEquals(identifierJson, patient.identifier[0].raw)
        assertEquals(0, patient.name.size)
        assertEquals(AdministrativeGender.MALE, patient.gender)
        assertEquals("1990-02-13", patient.birthDate)
        assertEquals(1, patient.telecom.size)
        assertEquals(telecomJson, patient.telecom[0].raw)
        assertEquals(1, patient.address.size)
        assertEquals(addressJson, patient.address[0].raw)
    }

    @Test
    fun `supports multiple names`() {
        val identifierJson = """{"system":"SSN","value":"123-45-6789"}"""
        val nameJson1 = """{"use":"usual"}"""
        val nameJson2 = """{"use":"maiden"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val addressJson = """{"state":"MO"}"""

        val json = """{
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson
            |   ],
            |   "name": [
            |      $nameJson1,
            |      $nameJson2
            |   ],
            |   "gender": "male",
            |   "birthDate": "1990-02-13",
            |   "telecom": [
            |      $telecomJson
            |   ],
            |   "address": [
            |      $addressJson
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(1, patient.identifier.size)
        assertEquals(identifierJson, patient.identifier[0].raw)
        assertEquals(2, patient.name.size)
        assertEquals(nameJson1, patient.name[0].raw)
        assertEquals(nameJson2, patient.name[1].raw)
        assertEquals(AdministrativeGender.MALE, patient.gender)
        assertEquals("1990-02-13", patient.birthDate)
        assertEquals(1, patient.telecom.size)
        assertEquals(telecomJson, patient.telecom[0].raw)
        assertEquals(1, patient.address.size)
        assertEquals(addressJson, patient.address[0].raw)
    }

    @Test
    fun `supports no gender`() {
        val identifierJson = """{"system":"SSN","value":"123-45-6789"}"""
        val nameJson = """{"use":"usual"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val addressJson = """{"state":"MO"}"""

        val json = """{
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson
            |   ],
            |   "name": [
            |      $nameJson
            |   ],
            |   "birthDate": "1990-02-13",
            |   "telecom": [
            |      $telecomJson
            |   ],
            |   "address": [
            |      $addressJson
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(1, patient.identifier.size)
        assertEquals(identifierJson, patient.identifier[0].raw)
        assertEquals(1, patient.name.size)
        assertEquals(nameJson, patient.name[0].raw)
        assertNull(patient.gender)
        assertEquals("1990-02-13", patient.birthDate)
        assertEquals(1, patient.telecom.size)
        assertEquals(telecomJson, patient.telecom[0].raw)
        assertEquals(1, patient.address.size)
        assertEquals(addressJson, patient.address[0].raw)
    }

    @Test
    fun `supports no birth date`() {
        val identifierJson = """{"system":"SSN","value":"123-45-6789"}"""
        val nameJson = """{"use":"usual"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val addressJson = """{"state":"MO"}"""

        val json = """{
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson
            |   ],
            |   "name": [
            |      $nameJson
            |   ],
            |   "gender": "male",
            |   "telecom": [
            |      $telecomJson
            |   ],
            |   "address": [
            |      $addressJson
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(1, patient.identifier.size)
        assertEquals(identifierJson, patient.identifier[0].raw)
        assertEquals(1, patient.name.size)
        assertEquals(nameJson, patient.name[0].raw)
        assertEquals(AdministrativeGender.MALE, patient.gender)
        assertNull(patient.birthDate)
        assertEquals(1, patient.telecom.size)
        assertEquals(telecomJson, patient.telecom[0].raw)
        assertEquals(1, patient.address.size)
        assertEquals(addressJson, patient.address[0].raw)
    }

    @Test
    fun `supports no telecom`() {
        val identifierJson = """{"system":"SSN","value":"123-45-6789"}"""
        val nameJson = """{"use":"usual"}"""
        val addressJson = """{"state":"MO"}"""

        val json = """{
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson
            |   ],
            |   "name": [
            |      $nameJson
            |   ],
            |   "gender": "male",
            |   "birthDate": "1990-02-13",
            |   "address": [
            |      $addressJson
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(1, patient.identifier.size)
        assertEquals(identifierJson, patient.identifier[0].raw)
        assertEquals(1, patient.name.size)
        assertEquals(nameJson, patient.name[0].raw)
        assertEquals(AdministrativeGender.MALE, patient.gender)
        assertEquals("1990-02-13", patient.birthDate)
        assertEquals(0, patient.telecom.size)
        assertEquals(1, patient.address.size)
        assertEquals(addressJson, patient.address[0].raw)
    }

    @Test
    fun `supports multiple telecoms`() {
        val identifierJson = """{"system":"SSN","value":"123-45-6789"}"""
        val nameJson = """{"use":"usual"}"""
        val telecomJson1 = """{"value":"123-456-7890"}"""
        val telecomJson2 = """{"value":"test@projectronin.com"}"""
        val addressJson = """{"state":"MO"}"""

        val json = """{
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson
            |   ],
            |   "name": [
            |      $nameJson
            |   ],
            |   "gender": "male",
            |   "birthDate": "1990-02-13",
            |   "telecom": [
            |      $telecomJson1,
            |      $telecomJson2
            |   ],
            |   "address": [
            |      $addressJson
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(1, patient.identifier.size)
        assertEquals(identifierJson, patient.identifier[0].raw)
        assertEquals(1, patient.name.size)
        assertEquals(nameJson, patient.name[0].raw)
        assertEquals(AdministrativeGender.MALE, patient.gender)
        assertEquals("1990-02-13", patient.birthDate)
        assertEquals(2, patient.telecom.size)
        assertEquals(telecomJson1, patient.telecom[0].raw)
        assertEquals(telecomJson2, patient.telecom[1].raw)
        assertEquals(1, patient.address.size)
        assertEquals(addressJson, patient.address[0].raw)
    }

    @Test
    fun `supports no address`() {
        val identifierJson = """{"system":"SSN","value":"123-45-6789"}"""
        val nameJson = """{"use":"usual"}"""
        val telecomJson = """{"value":"123-456-7890"}"""

        val json = """{
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson
            |   ],
            |   "name": [
            |      $nameJson
            |   ],
            |   "gender": "male",
            |   "birthDate": "1990-02-13",
            |   "telecom": [
            |      $telecomJson
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(1, patient.identifier.size)
        assertEquals(identifierJson, patient.identifier[0].raw)
        assertEquals(1, patient.name.size)
        assertEquals(nameJson, patient.name[0].raw)
        assertEquals(AdministrativeGender.MALE, patient.gender)
        assertEquals("1990-02-13", patient.birthDate)
        assertEquals(1, patient.telecom.size)
        assertEquals(telecomJson, patient.telecom[0].raw)
        assertEquals(0, patient.address.size)
    }

    @Test
    fun `supports multiple addresses`() {
        val identifierJson = """{"system":"SSN","value":"123-45-6789"}"""
        val nameJson = """{"use":"usual"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val addressJson1 = """{"state":"MO"}"""
        val addressJson2 = """{"state":"GA"}"""

        val json = """{
            |   "id": "1234567890",
            |   "identifier": [
            |      $identifierJson
            |   ],
            |   "name": [
            |      $nameJson
            |   ],
            |   "gender": "male",
            |   "birthDate": "1990-02-13",
            |   "telecom": [
            |      $telecomJson
            |   ],
            |   "address": [
            |      $addressJson1,
            |      $addressJson2
            |   ]
            |}""".trimMargin()

        val patient = EpicPatient(json)
        assertEquals(json, patient.raw)
        assertEquals(DataSource.FHIR_R4, patient.dataSource)
        assertEquals(ResourceType.PATIENT, patient.resourceType)
        assertEquals("1234567890", patient.id)
        assertEquals(1, patient.identifier.size)
        assertEquals(identifierJson, patient.identifier[0].raw)
        assertEquals(1, patient.name.size)
        assertEquals(nameJson, patient.name[0].raw)
        assertEquals(AdministrativeGender.MALE, patient.gender)
        assertEquals("1990-02-13", patient.birthDate)
        assertEquals(1, patient.telecom.size)
        assertEquals(telecomJson, patient.telecom[0].raw)
        assertEquals(2, patient.address.size)
        assertEquals(addressJson1, patient.address[0].raw)
        assertEquals(addressJson2, patient.address[1].raw)
    }
}
