package com.projectronin.interop.ehr.inputs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EHRMessageInputTest {
    @Test
    fun `create an input object`() {
        val actualInput = EHRMessageInput("Text", "MRN", listOf(EHRRecipient("ID", true)))

        assertEquals("Text", actualInput.text)
        assertEquals("MRN", actualInput.patientMRN)
        assertEquals("ID", actualInput.recipients[0].id)
        assertEquals(true, actualInput.recipients[0].isPool)
    }
}
