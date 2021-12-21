package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.enums.DataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicPractitionerRoleTest {
    @Test
    fun `can build from JSON`() {
        val practitionerJSON = """{"reference":"Practitioner/abc123","display":"John Adams, MD"}"""
        val codingJSON = """{"system":"abc123","code":"1","display":"Physician"}"""
        val locationJSON = """{"reference":"Location/abc123"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val specialtyJSON = """{"code":{"coding":[{"system":"abc","code":"11","display":"MD"}],"text":"MD"}}"""

        val json = """{
            |    "id": "xyz321",
            |    "active": true,
            |    "practitioner": $practitionerJSON,
            |    "code": [
            |      {
            |        "coding": [$codingJSON],
            |        "text": "Physician"
            |      }
            |    ],
            |    "location": [$locationJSON],
            |    "telecom": [$telecomJson],
            |    "specialty": [$specialtyJSON]
            |}""".trimMargin()

        val practitionerRole = EpicPractitionerRole(json)
        assertEquals(json, practitionerRole.raw)
        assertEquals(DataSource.FHIR_R4, practitionerRole.dataSource)
        assertEquals(ResourceType.PRACTITIONER_ROLE, practitionerRole.resourceType)
        assertEquals("xyz321", practitionerRole.id)
        assertEquals(true, practitionerRole.active)

        // Practitioner
        assertEquals(practitionerJSON, practitionerRole.practitioner?.raw)

        // Code
        assertEquals(1, practitionerRole.code.size)
        assertEquals(1, practitionerRole.code[0].coding.size)
        assertEquals(codingJSON, practitionerRole.code[0].coding[0].raw)
        assertEquals("Physician", practitionerRole.code[0].text)

        // Location
        assertEquals(1, practitionerRole.location.size)
        assertEquals(locationJSON, practitionerRole.location[0].raw)

        // Telecom
        assertEquals(1, practitionerRole.telecom.size)
        assertEquals(telecomJson, practitionerRole.telecom[0].raw)

        // Specialty
        assertEquals(1, practitionerRole.specialty.size)
        assertEquals(specialtyJSON, practitionerRole.specialty[0].raw)
    }

    @Test
    fun `can build with null values`() {
        val json = """{
            |    "id": "xyz321",
            |    "practitioner": null,
            |    "code": [
            |      {
            |        "text": "Physician"
            |      }
            |    ],
            |    "location": []
            |}""".trimMargin()

        val practitionerRole = EpicPractitionerRole(json)
        assertEquals(json, practitionerRole.raw)
        assertEquals(DataSource.FHIR_R4, practitionerRole.dataSource)
        assertEquals(ResourceType.PRACTITIONER_ROLE, practitionerRole.resourceType)
        assertEquals("xyz321", practitionerRole.id)
        assertNull(practitionerRole.active)

        // Practitioner
        assertNull(practitionerRole.practitioner?.raw)

        // Code
        assertEquals(1, practitionerRole.code.size)
        assertEquals(0, practitionerRole.code[0].coding.size)
        assertEquals("Physician", practitionerRole.code[0].text)

        // Location
        assertEquals(0, practitionerRole.location.size)
    }
}
