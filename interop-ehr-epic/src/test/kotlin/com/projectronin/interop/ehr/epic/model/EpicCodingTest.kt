package com.projectronin.interop.ehr.epic.model

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EpicCodingTest {
    @Test
    fun `can build from JSON`() {
        val json = """{
            |  "system": "urn:oid:1.2.840.114350.1.13.0.1.7.10.836982.1040",
            |  "version": "123",
            |  "code": "1",
            |  "display": "Physician",
            |  "userSelected": true
            |}""".trimMargin()

        val coding = EpicCoding(json)
        Assertions.assertEquals(json, coding.raw)
        Assertions.assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.10.836982.1040", coding.system)
        Assertions.assertEquals("123", coding.version)
        Assertions.assertEquals("1", coding.code)
        Assertions.assertEquals("Physician", coding.display)
        Assertions.assertEquals(true, coding.userSelected)
    }

    @Test
    fun `can build with null and missing values`() {
        val json = """{
            |  "system": "urn:oid:1.2.840.114350.1.13.0.1.7.10.836982.1040",
            |  "code": "1",
            |  "display": "Physician",
            |  "userSelected": null
            |}""".trimMargin()

        val coding = EpicCoding(json)
        Assertions.assertEquals(json, coding.raw)
        Assertions.assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.10.836982.1040", coding.system)
        Assertions.assertNull(coding.version)
        Assertions.assertEquals("1", coding.code)
        Assertions.assertEquals("Physician", coding.display)
        Assertions.assertNull(coding.userSelected)
    }
}
