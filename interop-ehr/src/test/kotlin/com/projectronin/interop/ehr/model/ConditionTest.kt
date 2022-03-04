package com.projectronin.interop.ehr.model

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConditionTest {
    @Test
    fun `creates date time onset`() {
        val onset = Condition.DateTimeOnset("2022-03-03")
        assertEquals("2022-03-03", onset.value)
    }

    @Test
    fun `creates age onset`() {
        val age = mockk<Age>()
        val onset = Condition.AgeOnset(age)
        assertEquals(age, onset.value)
    }

    @Test
    fun `creates period onset`() {
        val period = mockk<Period>()
        val onset = Condition.PeriodOnset(period)
        assertEquals(period, onset.value)
    }

    @Test
    fun `creates range onset`() {
        val range = mockk<Range>()
        val onset = Condition.RangeOnset(range)
        assertEquals(range, onset.value)
    }

    @Test
    fun `creates string onset`() {
        val onset = Condition.StringOnset("tomorrow")
        assertEquals("tomorrow", onset.value)
    }

    @Test
    fun `creates date time abatement`() {
        val abatement = Condition.DateTimeAbatement("2022-03-03")
        assertEquals("2022-03-03", abatement.value)
    }

    @Test
    fun `creates age abatement`() {
        val age = mockk<Age>()
        val abatement = Condition.AgeAbatement(age)
        assertEquals(age, abatement.value)
    }

    @Test
    fun `creates period abatement`() {
        val period = mockk<Period>()
        val abatement = Condition.PeriodAbatement(period)
        assertEquals(period, abatement.value)
    }

    @Test
    fun `creates range abatement`() {
        val range = mockk<Range>()
        val abatement = Condition.RangeAbatement(range)
        assertEquals(range, abatement.value)
    }

    @Test
    fun `creates string abatement`() {
        val abatement = Condition.StringAbatement("tomorrow")
        assertEquals("tomorrow", abatement.value)
    }
}
