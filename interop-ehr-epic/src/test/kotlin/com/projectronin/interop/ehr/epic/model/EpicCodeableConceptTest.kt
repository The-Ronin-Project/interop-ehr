package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicCodeableConceptTest {
    @Test
    fun `can build from object`() {
        val coding = Coding(
            system = Uri("abcd123"),
            code = Code("1"),
            display = "Physician"
        )
        val codeableConcept = CodeableConcept(
            coding = listOf(coding),
            text = "Physician"
        )

        val epicCodeableConcept = EpicCodeableConcept(codeableConcept)
        assertEquals(codeableConcept, epicCodeableConcept.element)
        assertEquals("Physician", epicCodeableConcept.text)
        assertEquals(coding, epicCodeableConcept.coding[0].element)
    }

    @Test
    fun `can build with null and missing values`() {
        val codeableConcept = CodeableConcept(
            coding = listOf(),
            text = null
        )

        val epicCodeableConcept = EpicCodeableConcept(codeableConcept)
        assertEquals(codeableConcept, epicCodeableConcept.element)
        Assertions.assertNull(epicCodeableConcept.text)
        assertEquals(0, epicCodeableConcept.coding.size)
    }

    @Test
    fun `returns JSON from raw`() {
        val coding = Coding(
            system = Uri("abcd123"),
            code = Code("1"),
            display = "Physician"
        )
        val codeableConcept = CodeableConcept(
            coding = listOf(coding),
            text = "Physician"
        )

        val codingJSON = """{"system":"abcd123","code":"1","display":"Physician"}"""

        val json = """{
            |"coding": [
            |   $codingJSON
            |   ],
            |   "text": "Physician"
            |}""".trimMargin()

        val epicCodeableConcept = EpicCodeableConcept(codeableConcept)
        assertEquals(codeableConcept, epicCodeableConcept.element)
        assertEquals(deformat(json), epicCodeableConcept.raw)
        assertEquals(coding, epicCodeableConcept.coding[0].element)
        assertEquals(codingJSON, epicCodeableConcept.coding[0].raw)
    }
}
