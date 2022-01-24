package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.enums.DataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicConditionTest {
    @Test
    fun `can build from JSON`() {
        val identifierJSON = """{"system":"abc123","value":"E14345"}"""
        val clinicalStatusJSON = """{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/condition-clinical","version":"4.0.0","code":"resolved","display":"Resolved"}],"text":"Resolved"}"""
        val verificationStatusJSON = """{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/condition-ver-status","version":"4.0.0","code":"confirmed","display":"Confirmed"}],"text":"Confirmed"}"""
        val categoryJSON = """{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/condition-category","code":"problem-list-item","display":"Problem List Item"}],"text":"Problem List Item"}"""
        val codeJSON = """{"coding":[{"system":"urn:oid:2.16.840.1.113883.6.96","code":"21522001"}],"text":"Abdominal pain"}"""

        val json = """
            {
                "resourceType": "Condition",
                "id": "eGVC1DSR9YDJxMi7Th3xbsA3",
                "identifier": [$identifierJSON],
                "clinicalStatus": $clinicalStatusJSON,
                "verificationStatus": $verificationStatusJSON,
                "category": [$categoryJSON],
                "code": $codeJSON
              }
        """.trimIndent()

        val condition = EpicCondition(json)
        assertEquals(json, condition.raw)
        assertEquals(DataSource.FHIR_R4, condition.dataSource)
        assertEquals(ResourceType.CONDITION, condition.resourceType)
        assertEquals("eGVC1DSR9YDJxMi7Th3xbsA3", condition.id)

        // Identifier
        assertEquals(1, condition.identifier.size)
        assertEquals(identifierJSON, condition.identifier[0].raw)

        // Clinical status
        assertEquals(clinicalStatusJSON, condition.clinicalStatus!!.raw)

        // Verification status
        assertEquals(verificationStatusJSON, condition.verificationStatus!!.raw)

        // Category
        assertEquals(1, condition.category.size)
        assertEquals(categoryJSON, condition.category[0].raw)

        // Code
        assertEquals(codeJSON, condition.code!!.raw)
    }

    @Test
    fun `can build from null and missing values`() {
        val json = """
            {
                "resourceType": "Condition",
                "id": "eGVC1DSR9YDJxMi7Th3xbsA3",
                "identifier": [],
                "category": [],
                "code": null
              }
        """.trimIndent()

        val condition = EpicCondition(json)
        assertEquals(json, condition.raw)
        assertEquals(DataSource.FHIR_R4, condition.dataSource)
        assertEquals(ResourceType.CONDITION, condition.resourceType)
        assertEquals("eGVC1DSR9YDJxMi7Th3xbsA3", condition.id)
        assertEquals(0, condition.identifier.size)
        assertNull(condition.clinicalStatus)
        assertNull(condition.verificationStatus)
        assertEquals(0, condition.category.size)
        assertNull(condition.code)
    }
}
