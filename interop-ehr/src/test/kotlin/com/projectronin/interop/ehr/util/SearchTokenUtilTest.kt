package com.projectronin.interop.ehr.util

import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.fhir.r4.CodeSystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SearchTokenUtilTest {
    @Test
    fun `toSearchTokens() forms a list of objects`() {
        val stringList = listOf("aaa|active", "resolved", "ccc|recurrence")
        val tokenList = stringList.toSearchTokens()
        assertEquals("aaa|active", tokenList[0].toParam())
        assertEquals("resolved", tokenList[1].toParam())
        assertEquals("ccc|recurrence", tokenList[2].toParam())
    }

    @Test
    fun `toParamValue() forms a comma-separated string`() {
        val clinicalSystem = CodeSystem.CONDITION_CLINICAL.uri.value
        val token1 = FHIRSearchToken(system = clinicalSystem, code = "active")
        val token2 = FHIRSearchToken(code = "resolved")
        val token3 = FHIRSearchToken(system = clinicalSystem, code = "recurrence")
        val tokenList = listOf(token1, token2, token3)
        assertEquals(
            "$clinicalSystem|active,resolved,$clinicalSystem|recurrence",
            tokenList.toOrParams()
        )
    }
}
