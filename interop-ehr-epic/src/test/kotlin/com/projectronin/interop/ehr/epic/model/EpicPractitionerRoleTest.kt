package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicPractitionerRoleTest {
    @Test
    fun `can build from object`() {
        val practitioner = Reference(
            reference = "Practitioner/abc123",
            display = "John Adams, MD"
        )
        val code = CodeableConcept(
            coding = listOf(Coding(system = Uri("abc123"), code = Code("1"), display = "Physician")),
            text = "Physician"
        )
        val location = Reference(reference = "Location/abc123")
        val telecom = ContactPoint(value = "123-456-7890")
        val specialty = CodeableConcept(
            coding = listOf(Coding(system = Uri("abc"), code = Code("11"), display = "MD")),
            text = "MD"
        )
        val practitionerRole = PractitionerRole(
            id = Id("xyz321"),
            active = true,
            practitioner = practitioner,
            code = listOf(code),
            location = listOf(location),
            telecom = listOf(telecom),
            specialty = listOf(specialty)
        )

        val epicPractitionerRole = EpicPractitionerRole(practitionerRole)
        assertEquals(practitionerRole, epicPractitionerRole.resource)
        assertEquals(DataSource.FHIR_R4, epicPractitionerRole.dataSource)
        assertEquals(ResourceType.PRACTITIONER_ROLE, epicPractitionerRole.resourceType)
        assertEquals("xyz321", epicPractitionerRole.id)
        assertEquals(true, epicPractitionerRole.active)

        // Practitioner
        assertEquals(practitioner, epicPractitionerRole.practitioner?.element)

        // Code
        assertEquals(1, epicPractitionerRole.code.size)
        assertEquals(code, epicPractitionerRole.code[0].element)

        // Location
        assertEquals(1, epicPractitionerRole.location.size)
        assertEquals(location, epicPractitionerRole.location[0].element)

        // Telecom
        assertEquals(1, epicPractitionerRole.telecom.size)
        assertEquals(telecom, epicPractitionerRole.telecom[0].element)

        // Specialty
        assertEquals(1, epicPractitionerRole.specialty.size)
        assertEquals(specialty, epicPractitionerRole.specialty[0].element)
    }

    @Test
    fun `can build with null values`() {
        val code = CodeableConcept(text = "Physician")
        val practitionerRole = PractitionerRole(
            id = Id("xyz321"),
            practitioner = null,
            code = listOf(code),
            location = listOf()
        )

        val epicPractitionerRole = EpicPractitionerRole(practitionerRole)
        assertEquals(practitionerRole, epicPractitionerRole.resource)
        assertEquals(DataSource.FHIR_R4, epicPractitionerRole.dataSource)
        assertEquals(ResourceType.PRACTITIONER_ROLE, epicPractitionerRole.resourceType)
        assertEquals("xyz321", epicPractitionerRole.id)
        assertNull(epicPractitionerRole.active)

        // Practitioner
        assertNull(epicPractitionerRole.practitioner?.element)

        // Code
        assertEquals(1, epicPractitionerRole.code.size)
        assertEquals(0, epicPractitionerRole.code[0].coding.size)
        assertEquals("Physician", epicPractitionerRole.code[0].text)

        // Location
        assertEquals(0, epicPractitionerRole.location.size)
    }

    @Test
    fun `returns JSON as raw`() {
        val practitioner = Reference(
            reference = "Practitioner/abc123",
            display = "John Adams, MD"
        )
        val code = CodeableConcept(
            coding = listOf(Coding(system = Uri("abc123"), code = Code("1"), display = "Physician")),
            text = "Physician"
        )
        val location = Reference(reference = "Location/abc123")
        val telecom = ContactPoint(value = "123-456-7890")
        val specialty = CodeableConcept(
            coding = listOf(Coding(system = Uri("abc"), code = Code("11"), display = "MD")),
            text = "MD"
        )
        val practitionerRole = PractitionerRole(
            id = Id("xyz321"),
            active = true,
            practitioner = practitioner,
            code = listOf(code),
            location = listOf(location),
            telecom = listOf(telecom),
            specialty = listOf(specialty)
        )

        val practitionerJSON = """{"reference":"Practitioner/abc123","display":"John Adams, MD"}"""
        val codeJSON = """{"coding":[{"system":"abc123","code":"1","display":"Physician"}],"text":"Physician"}"""
        val locationJSON = """{"reference":"Location/abc123"}"""
        val telecomJson = """{"value":"123-456-7890"}"""
        val specialtyJSON = """{"coding":[{"system":"abc","code":"11","display":"MD"}],"text":"MD"}"""

        val json = """{
            |    "resourceType" : "PractitionerRole",
            |    "id": "xyz321",
            |    "active": true,
            |    "practitioner": $practitionerJSON,
            |    "code": [$codeJSON],
            |    "specialty": [$specialtyJSON],
            |    "location": [$locationJSON],
            |    "telecom": [$telecomJson]
            |}""".trimMargin()

        val epicPractitionerRole = EpicPractitionerRole(practitionerRole)
        assertEquals(practitionerRole, epicPractitionerRole.resource)
        assertEquals(deformat(json), epicPractitionerRole.raw)

        // Practitioner
        assertEquals(practitioner, epicPractitionerRole.practitioner?.element)
        assertEquals(practitionerJSON, epicPractitionerRole.practitioner?.raw)

        // Code
        assertEquals(1, epicPractitionerRole.code.size)
        assertEquals(code, epicPractitionerRole.code[0].element)
        assertEquals(codeJSON, epicPractitionerRole.code[0].raw)

        // Location
        assertEquals(1, epicPractitionerRole.location.size)
        assertEquals(location, epicPractitionerRole.location[0].element)
        assertEquals(locationJSON, epicPractitionerRole.location[0].raw)

        // Telecom
        assertEquals(1, epicPractitionerRole.telecom.size)
        assertEquals(telecom, epicPractitionerRole.telecom[0].element)
        assertEquals(telecomJson, epicPractitionerRole.telecom[0].raw)

        // Specialty
        assertEquals(1, epicPractitionerRole.specialty.size)
        assertEquals(specialty, epicPractitionerRole.specialty[0].element)
        assertEquals(specialtyJSON, epicPractitionerRole.specialty[0].raw)
    }
}
