package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicContactPointTest {
    @Test
    fun `can build from object`() {
        val contactPoint = ContactPoint(
            system = ContactPointSystem.PHONE,
            value = "0648352638",
            use = ContactPointUse.MOBILE
        )

        val epicContactPoint = EpicContactPoint(contactPoint)
        assertEquals(contactPoint, epicContactPoint.element)
        assertEquals(ContactPointSystem.PHONE, epicContactPoint.system)
        assertEquals(ContactPointUse.MOBILE, epicContactPoint.use)
        assertEquals("0648352638", epicContactPoint.value)
    }

    @Test
    fun `supports no use`() {
        val contactPoint = ContactPoint(
            system = ContactPointSystem.PHONE,
            value = "0648352638"
        )

        val epicContactPoint = EpicContactPoint(contactPoint)
        assertEquals(contactPoint, epicContactPoint.element)
        assertEquals(ContactPointSystem.PHONE, epicContactPoint.system)
        assertNull(epicContactPoint.use)
        assertEquals("0648352638", epicContactPoint.value)
    }

    @Test
    fun `supports no value`() {
        val contactPoint = ContactPoint(
            system = ContactPointSystem.PHONE,
            use = ContactPointUse.MOBILE
        )

        val epicContactPoint = EpicContactPoint(contactPoint)
        assertEquals(contactPoint, epicContactPoint.element)
        assertEquals(ContactPointSystem.PHONE, epicContactPoint.system)
        assertEquals(ContactPointUse.MOBILE, epicContactPoint.use)
        assertNull(epicContactPoint.value)
    }

    @Test
    fun `returns JSON as raw`() {
        val contactPoint = ContactPoint(
            system = ContactPointSystem.PHONE,
            value = "0648352638",
            use = ContactPointUse.MOBILE
        )
        val json = """{
          |  "system": "phone",
          |  "value": "0648352638",
          |  "use": "mobile"
          |}""".trimMargin()

        val epicContactPoint = EpicContactPoint(contactPoint)
        assertEquals(contactPoint, epicContactPoint.element)
        assertEquals(deformat(json), epicContactPoint.raw)
    }
}
