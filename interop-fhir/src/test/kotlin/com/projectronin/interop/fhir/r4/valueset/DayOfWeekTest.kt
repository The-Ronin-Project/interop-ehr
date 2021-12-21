package com.projectronin.interop.fhir.r4.valueset

import com.projectronin.interop.common.enums.CodedEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DayOfWeekTest {
    @Test
    fun `can find enums by code`() {
        for (enum in DayOfWeek.values()) {
            assertEquals(enum, CodedEnum.byCode<DayOfWeek>(enum.code))
        }
    }
}
