package com.projectronin.interop.ehr.hl7.converters.datatypes

import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ContactPointUtilsTest {
    @Test
    fun `contactPoint conversion works`() {
        assertEquals("PRN", ContactPointUse.HOME.toHL7Code())
        assertEquals("WPN", ContactPointUse.WORK.toHL7Code())
        assertEquals("ORN", ContactPointUse.MOBILE.toHL7Code())
        assertNull(ContactPointUse.OLD.toHL7Code())
        assertNull(ContactPointUse.TEMPORARY.toHL7Code())
    }
}
