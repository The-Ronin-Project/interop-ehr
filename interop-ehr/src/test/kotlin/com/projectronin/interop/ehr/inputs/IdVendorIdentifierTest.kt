package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IdVendorIdentifierTest {
    @Test
    fun `can be created`() {
        val identifier = IdVendorIdentifier(Id("1234"))

        assertTrue(identifier.isFhirId)
        assertEquals(Id("1234"), identifier.identifier)
    }
}
