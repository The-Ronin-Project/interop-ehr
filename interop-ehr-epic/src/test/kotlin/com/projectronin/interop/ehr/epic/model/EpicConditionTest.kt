package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Condition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicConditionTest {
    @Test
    fun `can build from JSON`() {
        val identifier = Identifier(system = Uri("abc123"), value = "E14345")
        val clinicalStatus = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                    version = "4.0.0",
                    code = Code("resolved"),
                    display = "Resolved"
                )
            ),
            text = "Resolved"
        )
        val verificationStatus = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                    version = "4.0.0",
                    code = Code("confirmed"),
                    display = "Confirmed"
                )
            ),
            text = "Confirmed"
        )
        val category = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-category"),
                    code = Code("problem-list-item"),
                    display = "Problem List Item"
                )
            ),
            text = "Problem List Item"
        )
        val code = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("urn:oid:2.16.840.1.113883.6.96"),
                    code = Code("21522001"),
                )
            ),
            text = "Abdominal pain"
        )

        val condition = Condition(
            id = Id("eGVC1DSR9YDJxMi7Th3xbsA3"),
            identifier = listOf(identifier),
            clinicalStatus = clinicalStatus,
            verificationStatus = verificationStatus,
            category = listOf(category),
            code = code,
            subject = Reference(id = "1234")
        )

        val epicCondition = EpicCondition(condition)
        assertEquals(condition, epicCondition.resource)
        assertEquals(DataSource.FHIR_R4, epicCondition.dataSource)
        assertEquals(ResourceType.CONDITION, epicCondition.resourceType)
        assertEquals("eGVC1DSR9YDJxMi7Th3xbsA3", epicCondition.id)

        // Identifier
        assertEquals(1, epicCondition.identifier.size)
        assertEquals(identifier, epicCondition.identifier[0].element)

        // Clinical status
        assertEquals(clinicalStatus, epicCondition.clinicalStatus!!.element)

        // Verification status
        assertEquals(verificationStatus, epicCondition.verificationStatus!!.element)

        // Category
        assertEquals(1, epicCondition.category.size)
        assertEquals(category, epicCondition.category[0].element)

        // Code
        assertEquals(code, epicCondition.code!!.element)
    }

    @Test
    fun `can build from null and missing values`() {
        val condition = Condition(
            id = Id("eGVC1DSR9YDJxMi7Th3xbsA3"),
            identifier = listOf(),
            category = listOf(),
            code = null,
            subject = Reference(id = "1234")
        )

        val epicCondition = EpicCondition(condition)
        assertEquals(condition, epicCondition.resource)
        assertEquals(DataSource.FHIR_R4, epicCondition.dataSource)
        assertEquals(ResourceType.CONDITION, epicCondition.resourceType)
        assertEquals("eGVC1DSR9YDJxMi7Th3xbsA3", epicCondition.id)
        assertEquals(0, epicCondition.identifier.size)
        assertNull(epicCondition.clinicalStatus)
        assertNull(epicCondition.verificationStatus)
        assertEquals(0, epicCondition.category.size)
        assertNull(epicCondition.code)
    }

    @Test
    fun `return JSON from raw`() {
        val identifier = Identifier(system = Uri("abc123"), value = "E14345")
        val clinicalStatus = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                    version = "4.0.0",
                    code = Code("resolved"),
                    display = "Resolved"
                )
            ),
            text = "Resolved"
        )
        val verificationStatus = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                    version = "4.0.0",
                    code = Code("confirmed"),
                    display = "Confirmed"
                )
            ),
            text = "Confirmed"
        )
        val category = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/condition-category"),
                    code = Code("problem-list-item"),
                    display = "Problem List Item"
                )
            ),
            text = "Problem List Item"
        )
        val code = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("urn:oid:2.16.840.1.113883.6.96"),
                    code = Code("21522001"),
                )
            ),
            text = "Abdominal pain"
        )

        val condition = Condition(
            id = Id("eGVC1DSR9YDJxMi7Th3xbsA3"),
            identifier = listOf(identifier),
            clinicalStatus = clinicalStatus,
            verificationStatus = verificationStatus,
            category = listOf(category),
            code = code,
            subject = Reference(id = "1234")
        )

        val identifierJSON = """{"system":"abc123","value":"E14345"}"""
        val clinicalStatusJSON =
            """{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/condition-clinical","version":"4.0.0","code":"resolved","display":"Resolved"}],"text":"Resolved"}"""
        val verificationStatusJSON =
            """{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/condition-ver-status","version":"4.0.0","code":"confirmed","display":"Confirmed"}],"text":"Confirmed"}"""
        val categoryJSON =
            """{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/condition-category","code":"problem-list-item","display":"Problem List Item"}],"text":"Problem List Item"}"""
        val codeJSON =
            """{"coding":[{"system":"urn:oid:2.16.840.1.113883.6.96","code":"21522001"}],"text":"Abdominal pain"}"""

        val json = """
            {
                "resourceType": "Condition",
                "id": "eGVC1DSR9YDJxMi7Th3xbsA3",
                "identifier": [$identifierJSON],
                "clinicalStatus": $clinicalStatusJSON,
                "verificationStatus": $verificationStatusJSON,
                "category": [$categoryJSON],
                "code": $codeJSON,
                "subject" : {
                  "id": "1234"
                }
              }
        """.trimIndent()

        val epicCondition = EpicCondition(condition)
        assertEquals(condition, epicCondition.resource)
        assertEquals(deformat(json), epicCondition.raw)

        // Identifier
        assertEquals(1, epicCondition.identifier.size)
        assertEquals(identifier, epicCondition.identifier[0].element)
        assertEquals(identifierJSON, epicCondition.identifier[0].raw)

        // Clinical status
        assertEquals(clinicalStatus, epicCondition.clinicalStatus!!.element)
        assertEquals(clinicalStatusJSON, epicCondition.clinicalStatus!!.raw)

        // Verification status
        assertEquals(verificationStatus, epicCondition.verificationStatus!!.element)
        assertEquals(verificationStatusJSON, epicCondition.verificationStatus!!.raw)

        // Category
        assertEquals(1, epicCondition.category.size)
        assertEquals(category, epicCondition.category[0].element)
        assertEquals(categoryJSON, epicCondition.category[0].raw)

        // Code
        assertEquals(code, epicCondition.code!!.element)
        assertEquals(codeJSON, epicCondition.code!!.raw)
    }
}
