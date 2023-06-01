package com.interop.ehr.hl7.fhir.converters.datatypes

import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstantUtilsTest {
    @Test
    fun `formats ok`() {
        val inst = Instant(value = "2015-02-07T13:28:17.239+02:00")
        val inst2 = Instant(value = "2015-02-07T13:28:17.239Z")
        assertEquals("20150207132817", inst.toFormattedDate())
        assertEquals("20150207132817", inst2.toFormattedDate())
    }
}
