package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicCodingTest {
    @Test
    fun `can build from object`() {
        val coding = Coding(
            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.10.836982.1040"),
            version = "123",
            code = Code("1"),
            display = "Physician",
            userSelected = true
        )

        val epicCoding = EpicCoding(coding)
        assertEquals(coding, epicCoding.element)
        assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.10.836982.1040", epicCoding.system)
        assertEquals("123", epicCoding.version)
        assertEquals("1", epicCoding.code)
        assertEquals("Physician", epicCoding.display)
        assertEquals(true, epicCoding.userSelected)
    }

    @Test
    fun `can build with null and missing values`() {
        val coding = Coding(
            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.10.836982.1040"),
            code = Code("1"),
            display = "Physician",
            userSelected = null
        )

        val epicCoding = EpicCoding(coding)
        assertEquals(coding, epicCoding.element)
        assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.10.836982.1040", epicCoding.system)
        assertNull(epicCoding.version)
        assertEquals("1", epicCoding.code)
        assertEquals("Physician", epicCoding.display)
        assertNull(epicCoding.userSelected)
    }

    @Test
    fun `returns JSON from raw`() {
        val coding = Coding(
            system = Uri("urn:oid:1.2.840.114350.1.13.0.1.7.10.836982.1040"),
            version = "123",
            code = Code("1"),
            display = "Physician",
            userSelected = true
        )
        val json = """{
            |  "system": "urn:oid:1.2.840.114350.1.13.0.1.7.10.836982.1040",
            |  "version": "123",
            |  "code": "1",
            |  "display": "Physician",
            |  "userSelected": true
            |}""".trimMargin()

        val epicCoding = EpicCoding(coding)
        assertEquals(coding, epicCoding.element)
        assertEquals(deformat(json), epicCoding.raw)
    }
}
