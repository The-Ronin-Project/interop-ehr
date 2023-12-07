package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.ronin.generators.resource.observation.possibleDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninEffectiveUtilTest {
    @Test
    fun `keep provided effective element`() {
        val effective =
            DynamicValue<Any>(
                DynamicValueType.STRING,
                "something here",
            )
        val roninEffective = generateEffectiveDateTime(effective, possibleDateTime)
        assertEquals(roninEffective, effective)
    }

    @Test
    fun `generate effective element if none provided`() {
        val roninEffective = generateEffectiveDateTime(null, possibleDateTime)
        assertEquals(roninEffective, possibleDateTime)
    }
}
