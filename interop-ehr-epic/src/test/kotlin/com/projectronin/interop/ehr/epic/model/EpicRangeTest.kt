package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.Range
import com.projectronin.interop.fhir.r4.datatype.SimpleQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EpicRangeTest {
    @Test
    fun `can build from object`() {
        val low = SimpleQuantity(value = 15.3)
        val high = SimpleQuantity(value = 469.3)
        val range = Range(low = low, high = high)

        val epicRange = EpicRange(range)
        assertEquals(range, epicRange.element)
        assertEquals(low, epicRange.low?.element)
        assertEquals(high, epicRange.high?.element)
    }

    @Test
    fun `supports no low`() {
        val high = SimpleQuantity(value = 469.3)
        val range = Range(high = high)

        val epicRange = EpicRange(range)
        assertEquals(range, epicRange.element)
        assertNull(epicRange.low)
        assertEquals(high, epicRange.high?.element)
    }

    @Test
    fun `supports no high`() {
        val low = SimpleQuantity(value = 15.3)
        val range = Range(low = low)

        val epicRange = EpicRange(range)
        assertEquals(range, epicRange.element)
        assertEquals(low, epicRange.low?.element)
        assertNull(epicRange.high)
    }

    @Test
    fun `returns JSON as raw`() {
        val low = SimpleQuantity(value = 15.3)
        val high = SimpleQuantity(value = 469.3)
        val range = Range(low = low, high = high)

        val lowJSON = """{"value":15.3}"""
        val highJSON = """{"value":469.3}"""
        val json = """{"low":$lowJSON,"high":$highJSON}"""

        val epicRange = EpicRange(range)
        assertEquals(range, epicRange.element)
        assertEquals(deformat(json), epicRange.raw)

        assertEquals(low, epicRange.low?.element)
        assertEquals(lowJSON, epicRange.low?.raw)

        assertEquals(high, epicRange.high?.element)
        assertEquals(highJSON, epicRange.high?.raw)
    }
}
