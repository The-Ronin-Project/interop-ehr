package com.projectronin.interop.ehr.model

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObservationTest {
    @Test
    fun `creates codeable concept value`() {
        val code = mockk<CodeableConcept>()
        val value = Observation.ValueCodeableConcept(code)
        assertEquals(code, value.value)
    }

    @Test
    fun `creates quantity value`() {
        val quantity = mockk<SimpleQuantity>()
        val value = Observation.ValueQuantity(quantity)
        assertEquals(quantity, value.value)
    }

    @Test
    fun `creates range value`() {
        val range = mockk<Range>()
        val value = Observation.ValueRange(range)
        assertEquals(range, value.value)
    }

    @Test
    fun `creates ratio value`() {
        val ratio = mockk<Ratio>()
        val value = Observation.ValueRatio(ratio)
        assertEquals(ratio, value.value)
    }

    @Test
    fun `creates string value`() {
        val value = Observation.ValueString("tomorrow")
        assertEquals("tomorrow", value.value)
    }

    @Test
    fun `creates date time effective`() {
        val abatement = Observation.EffectiveDateTime("2022-03-03")
        assertEquals("2022-03-03", abatement.value)
    }
}
