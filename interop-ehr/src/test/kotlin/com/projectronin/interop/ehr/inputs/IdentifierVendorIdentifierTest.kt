package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class IdentifierVendorIdentifierTest {
    @Test
    fun `can be created`() {
        val identifier = IdentifierVendorIdentifier(Identifier(system = Uri("system"), value = "1234".asFHIR()))

        assertFalse(identifier.isFhirId)
        assertEquals(Identifier(system = Uri("system"), value = "1234".asFHIR()), identifier.identifier)
    }
}
