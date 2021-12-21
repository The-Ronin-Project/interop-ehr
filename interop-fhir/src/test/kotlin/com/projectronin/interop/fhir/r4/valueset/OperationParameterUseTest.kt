package com.projectronin.interop.fhir.r4.valueset

import com.projectronin.interop.common.enums.CodedEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OperationParameterUseTest {
    @Test
    fun `can find enums by code`() {
        for (enum in OperationParameterUse.values()) {
            assertEquals(enum, CodedEnum.byCode<OperationParameterUse>(enum.code))
        }
    }
}
