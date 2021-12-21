package com.projectronin.interop.fhir.r4.valueset

import com.projectronin.interop.common.enums.CodedEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NameUseTest {
    @Test
    fun `can find enums by code`() {
        for (enum in NameUse.values()) {
            assertEquals(enum, CodedEnum.byCode<NameUse>(enum.code))
        }
    }

    @Test
    fun `maiden has parent`() {
        assertEquals(NameUse.OLD, NameUse.MAIDEN.parent)
    }

    @Test
    fun `other enums have no parent`() {
        for (enum in NameUse.values()) {
            if (enum == NameUse.MAIDEN) {
                continue
            }

            assertNull(enum.parent)
        }
    }
}
