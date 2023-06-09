package com.projectronin.interop.ehr.hl7.converters.datatypes

import ca.uhn.hl7v2.model.v251.message.MDM_T02
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdentifierUtilsTest {
    @Test
    fun works() {
        val genericMessage = MDM_T02()
        val identifier = Identifier(
            value = "123".asFHIR(),
            type = CodeableConcept(text = "text".asFHIR())
        )
        val cx = identifier.toPID3(genericMessage)

        assertEquals("123", cx.idNumber.value)
        assertEquals("text", cx.assigningAuthority.namespaceID.value)
    }

    @Test
    fun `works with default`() {
        val genericMessage = MDM_T02()
        val identifier = Identifier(
            value = "123".asFHIR()
        )
        val cx = identifier.toPID3(genericMessage, "MRN")

        assertEquals("123", cx.idNumber.value)
        assertEquals("MRN", cx.assigningAuthority.namespaceID.value)
    }
}
