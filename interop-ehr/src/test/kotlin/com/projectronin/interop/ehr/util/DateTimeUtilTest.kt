package com.projectronin.interop.ehr.util

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal class DateTimeUtilTest {

    @Test
    fun `dateInDaysFromToday() returns`() {
        val mockDateFormat = mockk<DateTimeFormatter> {
            every { format(any<LocalTime>()) } returns "2003-04-05"
        }
        assertEquals("2003-04-05", daysToPastDate(60, mockDateFormat))
    }
}
