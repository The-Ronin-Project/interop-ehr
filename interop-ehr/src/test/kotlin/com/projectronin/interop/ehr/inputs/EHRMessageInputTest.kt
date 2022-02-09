package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.datatype.Identifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EHRMessageInputTest {
    @Test
    fun `create an input object`() {
        val actualInput =
            EHRMessageInput("Text", "MRN", listOf(EHRRecipient("ID", listOf(Identifier(value = "Ident1")))))

        assertEquals("Text", actualInput.text)
        assertEquals("MRN", actualInput.patientMRN)
        assertEquals("ID", actualInput.recipients[0].id)
        assertEquals("Ident1", actualInput.recipients[0].identifiers[0].value)
    }
}
