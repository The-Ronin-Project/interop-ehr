package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicContactPointTest {
    @Test
    fun `can build from JSON`() {
        val json = """{
          |  "system": "phone",
          |  "value": "0648352638",
          |  "use": "mobile"
          |}""".trimMargin()

        val contactPoint = EpicContactPoint(json)
        assertEquals(json, contactPoint.raw)
        assertEquals(ContactPointSystem.PHONE, contactPoint.system)
        assertEquals(ContactPointUse.MOBILE, contactPoint.use)
        assertEquals("0648352638", contactPoint.value)
    }

    @Test
    fun `supports no system`() {
        val json = """{
          |  "value": "0648352638",
          |  "use": "mobile"
          |}""".trimMargin()

        val contactPoint = EpicContactPoint(json)
        assertEquals(json, contactPoint.raw)
        assertNull(contactPoint.system)
        assertEquals(ContactPointUse.MOBILE, contactPoint.use)
        assertEquals("0648352638", contactPoint.value)
    }

    @Test
    fun `supports no use`() {
        val json = """{
          |  "system": "phone",
          |  "value": "0648352638"
          |}""".trimMargin()

        val contactPoint = EpicContactPoint(json)
        assertEquals(json, contactPoint.raw)
        assertEquals(ContactPointSystem.PHONE, contactPoint.system)
        assertNull(contactPoint.use)
        assertEquals("0648352638", contactPoint.value)
    }

    @Test
    fun `supports no value`() {
        val json = """{
          |  "system": "phone",
          |  "use": "mobile"
          |}""".trimMargin()

        val contactPoint = EpicContactPoint(json)
        assertEquals(json, contactPoint.raw)
        assertEquals(ContactPointSystem.PHONE, contactPoint.system)
        assertEquals(ContactPointUse.MOBILE, contactPoint.use)
        assertNull(contactPoint.value)
    }
}
