package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.NameUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicPractitionerTest {
    @Test
    fun `can build from object`() {
        val identifier = Identifier(system = Uri("abc123"), value = "E14345")
        val name = HumanName(use = NameUse.USUAL, family = "Staywell", given = listOf("Physician"))
        val communication = CodeableConcept(
            coding = listOf(Coding(system = Uri("abc"), code = Code("11"), display = "English")),
            text = "English"
        )
        val telecom = ContactPoint(value = "123-456-7890")
        val practitioner = Practitioner(
            id = Id("eYQbg.zDI2aP7oRI8LT.BzA3"),
            identifier = listOf(identifier),
            active = true,
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            telecom = listOf(telecom),
            communication = listOf(communication)
        )

        val epicPractitioner = EpicPractitioner(practitioner)
        assertEquals(practitioner, epicPractitioner.resource)
        assertEquals(DataSource.FHIR_R4, epicPractitioner.dataSource)
        assertEquals(ResourceType.PRACTITIONER, epicPractitioner.resourceType)
        assertEquals("eYQbg.zDI2aP7oRI8LT.BzA3", epicPractitioner.id)
        assertEquals(true, epicPractitioner.active)
        assertEquals(AdministrativeGender.MALE, epicPractitioner.gender)

        // Identifier
        assertEquals(1, epicPractitioner.identifier.size)
        assertEquals(identifier, epicPractitioner.identifier[0].element)
        assertEquals("abc123", epicPractitioner.identifier[0].system)
        assertEquals("E14345", epicPractitioner.identifier[0].value)

        // Name
        assertEquals(1, epicPractitioner.name.size)
        assertEquals(name, epicPractitioner.name[0].element)
        assertEquals(NameUse.USUAL, epicPractitioner.name[0].use)
        assertEquals("Staywell", epicPractitioner.name[0].family)
        assertEquals(1, epicPractitioner.name[0].given.size)
        assertEquals("Physician", epicPractitioner.name[0].given[0])

        // Telecom
        assertEquals(1, epicPractitioner.telecom.size)
        assertEquals(telecom, epicPractitioner.telecom[0].element)

        // Communication
        assertEquals(1, epicPractitioner.communication.size)
        assertEquals(communication, epicPractitioner.communication[0].element)
    }

    @Test
    fun `can build from null and missing values`() {
        val name = HumanName(use = NameUse.USUAL, family = "Staywell")
        val practitioner = Practitioner(
            id = Id("eYQbg.zDI2aP7oRI8LT.BzA3"),
            identifier = listOf(),
            name = listOf(name),
            gender = AdministrativeGender.MALE
        )

        val epicPractitioner = EpicPractitioner(practitioner)
        assertEquals(practitioner, epicPractitioner.resource)
        assertEquals(DataSource.FHIR_R4, epicPractitioner.dataSource)
        assertEquals(ResourceType.PRACTITIONER, epicPractitioner.resourceType)
        assertEquals("eYQbg.zDI2aP7oRI8LT.BzA3", epicPractitioner.id)
        assertNull(epicPractitioner.active)

        // Identifier
        assertEquals(0, epicPractitioner.identifier.size)

        // Name
        assertEquals(1, epicPractitioner.name.size)
        assertEquals(NameUse.USUAL, epicPractitioner.name[0].use)
        assertEquals("Staywell", epicPractitioner.name[0].family)
        assertEquals(0, epicPractitioner.name[0].given.size)
    }

    @Test
    fun `return JSON as raw`() {
        val identifier = Identifier(system = Uri("abc123"), value = "E14345")
        val name = HumanName(use = NameUse.USUAL, family = "Staywell", given = listOf("Physician"))
        val communication = CodeableConcept(
            coding = listOf(Coding(system = Uri("abc"), code = Code("11"), display = "English")),
            text = "English"
        )
        val telecom = ContactPoint(value = "123-456-7890")
        val practitioner = Practitioner(
            id = Id("eYQbg.zDI2aP7oRI8LT.BzA3"),
            identifier = listOf(identifier),
            active = true,
            name = listOf(name),
            gender = AdministrativeGender.MALE,
            telecom = listOf(telecom),
            communication = listOf(communication)
        )

        val identifierJSON = """{"system":"abc123","value":"E14345"}"""
        val nameJSON = """{"use":"usual","family":"Staywell","given":["Physician"]}"""
        val communicationJSON =
            """{"coding":[{"system":"abc","code":"11","display":"English"}],"text":"English"}"""
        val telecomJson = """{"value":"123-456-7890"}"""

        val json = """{
            |"resourceType": "Practitioner",
            |"id": "eYQbg.zDI2aP7oRI8LT.BzA3",
            |"identifier": [$identifierJSON],
            |"active": true,
            |"name": [$nameJSON],
            |"telecom": [$telecomJson],
            |"gender": "male",
            |"communication":[$communicationJSON]
            |}""".trimMargin()

        val epicPractitioner = EpicPractitioner(practitioner)
        assertEquals(practitioner, epicPractitioner.resource)
        assertEquals(deformat(json), epicPractitioner.raw)

        // Identifier
        assertEquals(1, epicPractitioner.identifier.size)
        assertEquals(identifier, epicPractitioner.identifier[0].element)
        assertEquals(identifierJSON, epicPractitioner.identifier[0].raw)

        // Name
        assertEquals(1, epicPractitioner.name.size)
        assertEquals(name, epicPractitioner.name[0].element)
        assertEquals(nameJSON, epicPractitioner.name[0].raw)

        // Telecom
        assertEquals(1, epicPractitioner.telecom.size)
        assertEquals(telecom, epicPractitioner.telecom[0].element)
        assertEquals(telecomJson, epicPractitioner.telecom[0].raw)

        // Communication
        assertEquals(1, epicPractitioner.communication.size)
        assertEquals(communication, epicPractitioner.communication[0].element)
        assertEquals(communicationJSON, epicPractitioner.communication[0].raw)
    }
}
