package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.ronin.generators.resource.possibleLocationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninCodeUtilTest {
    @Test
    fun `generate rcdm code when needed`() {
        val code = Code(null)
        val roninCode = generateCode(code, possibleLocationStatus.random())
        assertTrue(possibleLocationStatus.contains(roninCode))
    }

    @Test
    fun `use provided rcdm code for status`() {
        val code = Code("this is a code")
        val roninCode = generateCode(code, possibleLocationStatus.random())
        assertEquals(roninCode.value, "this is a code")
    }
}
