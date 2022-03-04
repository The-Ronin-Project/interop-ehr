package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicPeriodTest {
    @Test
    fun `can build from object`() {
        val period = Period(
            start = DateTime("2021-01-01"),
            end = DateTime("2021-12-31")
        )

        val epicPeriod = EpicPeriod(period)
        assertEquals(period, epicPeriod.element)
        assertEquals("2021-01-01", epicPeriod.start)
        assertEquals("2021-12-31", epicPeriod.end)
    }

    @Test
    fun `supports no start`() {
        val period = Period(
            end = DateTime("2021-12-31")
        )

        val epicPeriod = EpicPeriod(period)
        assertEquals(period, epicPeriod.element)
        assertNull(epicPeriod.start)
        assertEquals("2021-12-31", epicPeriod.end)
    }

    @Test
    fun `supports no end`() {
        val period = Period(
            start = DateTime("2021-01-01")
        )

        val epicPeriod = EpicPeriod(period)
        assertEquals(period, epicPeriod.element)
        assertEquals("2021-01-01", epicPeriod.start)
        assertNull(epicPeriod.end)
    }

    @Test
    fun `returns JSON as raw`() {
        val period = Period(
            start = DateTime("2021-01-01"),
            end = DateTime("2021-12-31")
        )

        val json = """{"start":"2021-01-01","end":"2021-12-31"}"""

        val epicPeriod = EpicPeriod(period)
        assertEquals(period, epicPeriod.element)
        assertEquals(deformat(json), epicPeriod.raw)
    }
}
