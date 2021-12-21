package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.NameUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicPractitionerTest {
    @Test
    fun `can build from JSON`() {
        val identifierJSON = """{"system":"abc123","value":"E14345"}"""
        val nameJSON = """{"use":"usual","family":"Staywell","given":["Physician"]}"""
        val qualificationJSON = """{"code":{"coding":[{"system":"abc","code":"11","display":"MD"}],"text":"MD"}}"""
        val communicationJSON = """{"code":{"coding":[{"system":"abc","code":"11","display":"English"}],"text":"English"}}"""
        val telecomJson = """{"value":"123-456-7890"}"""

        val json = """{
            |"resourceType": "Practitioner",
            |"id": "eYQbg.zDI2aP7oRI8LT.BzA3",
            |"identifier": [$identifierJSON],
            |"active": true,
            |"name": [$nameJSON],
            |"gender": "male",
            |"qualification": [$qualificationJSON]
            |"telecom": [$telecomJson]
            |"communication":[$communicationJSON]
            |}""".trimMargin()

        val practitioner = EpicPractitioner(json)
        assertEquals(json, practitioner.raw)
        assertEquals(DataSource.FHIR_R4, practitioner.dataSource)
        assertEquals(ResourceType.PRACTITIONER, practitioner.resourceType)
        assertEquals("eYQbg.zDI2aP7oRI8LT.BzA3", practitioner.id)
        assertEquals(true, practitioner.active)
        assertEquals(AdministrativeGender.MALE, practitioner.gender)

        // Identifier
        assertEquals(1, practitioner.identifier.size)
        assertEquals(identifierJSON, practitioner.identifier[0].raw)
        assertEquals("abc123", practitioner.identifier[0].system)
        assertEquals("E14345", practitioner.identifier[0].value)

        // Name
        assertEquals(1, practitioner.name.size)
        assertEquals(nameJSON, practitioner.name[0].raw)
        assertEquals(NameUse.USUAL, practitioner.name[0].use)
        assertEquals("Staywell", practitioner.name[0].family)
        assertEquals(1, practitioner.name[0].given.size)
        assertEquals("Physician", practitioner.name[0].given[0])

        // Qualification
        assertEquals(1, practitioner.qualification.size)
        assertEquals(qualificationJSON, practitioner.qualification[0].raw)

        // Telecom
        assertEquals(1, practitioner.telecom.size)
        assertEquals(telecomJson, practitioner.telecom[0].raw)

        // Communication
        assertEquals(1, practitioner.communication.size)
        assertEquals(communicationJSON, practitioner.communication[0].raw)
    }

    @Test
    fun `can build from null and missing values`() {
        val nameJSON = """{"use":"usual","family":"Staywell","given":[]}"""

        val json = """{
            |"resourceType": "Practitioner",
            |"id": "eYQbg.zDI2aP7oRI8LT.BzA3",
            |"identifier": [],
            |"active": null,
            |"name": [$nameJSON],
            |"gender": "male",
            |"qualification": []
            |}""".trimMargin()

        val practitioner = EpicPractitioner(json)
        assertEquals(json, practitioner.raw)
        assertEquals(DataSource.FHIR_R4, practitioner.dataSource)
        assertEquals(ResourceType.PRACTITIONER, practitioner.resourceType)
        assertEquals("eYQbg.zDI2aP7oRI8LT.BzA3", practitioner.id)
        assertNull(practitioner.active)

        // Identifier
        assertEquals(0, practitioner.identifier.size)

        // Name
        assertEquals(1, practitioner.name.size)
        assertEquals(nameJSON, practitioner.name[0].raw)
        assertEquals(NameUse.USUAL, practitioner.name[0].use)
        assertEquals("Staywell", practitioner.name[0].family)
        assertEquals(0, practitioner.name[0].given.size)

        // Qualification
        assertEquals(0, practitioner.qualification.size)
    }
}
