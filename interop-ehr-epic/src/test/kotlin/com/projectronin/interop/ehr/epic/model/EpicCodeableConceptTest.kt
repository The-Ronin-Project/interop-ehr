package com.projectronin.interop.ehr.epic.model

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EpicCodeableConceptTest {
    @Test
    fun `can build from JSON`() {
        val codingJSON = """{"system":"abcd123","code":"1","display":"Physician"}"""

        val json = """{
            |"coding": [
            |   $codingJSON
            |   ],
            |   "text": "Physician"
            |}""".trimMargin()

        val codeableConcept = EpicCodeableConcept(json)
        Assertions.assertEquals(json, codeableConcept.raw)
        Assertions.assertEquals("Physician", codeableConcept.text)
        Assertions.assertEquals(codingJSON, codeableConcept.coding[0].raw)
    }

    @Test
    fun `can build with null and missing values`() {
        val json = """{
            |"coding": [],
            |   "text": null
            |}""".trimMargin()

        val codeableConcept = EpicCodeableConcept(json)
        Assertions.assertEquals(json, codeableConcept.raw)
        Assertions.assertNull(codeableConcept.text)
        Assertions.assertEquals(0, codeableConcept.coding.size)
    }
}
