package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EHRMessageInputTest {
    @Test
    fun `create an input object`() {
        val actualInput =
            EHRMessageInput(
                text = "Text",
                patientMRN = "MRN",
                recipients = listOf(
                    EHRRecipient(
                        "ID",
                        IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "Ident1".asFHIR()))
                    )
                )
            )

        assertEquals("Text", actualInput.text)
        assertEquals("MRN", actualInput.patientMRN)
        assertEquals("ID", actualInput.recipients[0].id)

        val identifier = actualInput.recipients[0].identifier as IdentifierVendorIdentifier
        assertEquals("Ident1".asFHIR(), identifier.identifier.value)
    }
}
