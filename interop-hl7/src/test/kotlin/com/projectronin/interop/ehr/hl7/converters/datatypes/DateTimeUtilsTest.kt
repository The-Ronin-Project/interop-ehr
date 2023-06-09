package com.projectronin.interop.ehr.hl7.converters.datatypes

import com.interop.ehr.hl7.fhir.converters.datatypes.toFormattedDate
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DateTimeUtilsTest {
    @Test
    fun `formats ok`() {
        val dt1 = Date(value = "2015-02-07")
        val dt2 = Date(value = "2015-02")
        val dt3 = Date(value = "2015")
        assertEquals("20150207", dt1.toFormattedDate())
        assertEquals("201502", dt2.toFormattedDate())
        assertEquals("2015", dt3.toFormattedDate())
    }
}
