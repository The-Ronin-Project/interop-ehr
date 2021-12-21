package com.projectronin.interop.fhir.r4.valueset

import com.projectronin.interop.common.enums.CodedEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventTimingTest {
    @Test
    fun `can find enums by code`() {
        for (enum in EventTiming.values()) {
            assertEquals(enum, CodedEnum.byCode<EventTiming>(enum.code))
        }
    }
}
