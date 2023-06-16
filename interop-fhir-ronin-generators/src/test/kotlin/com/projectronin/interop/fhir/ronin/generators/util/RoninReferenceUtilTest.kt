package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninReferenceUtilTest {
    @Test
    fun `generate rcdm reference`() {
        val roninRef = rcdmReference("Patient", "1234")
        assertEquals(roninRef.type?.value, "Patient")
        assertEquals(roninRef.reference, "Patient/1234".asFHIR())
        assertEquals(roninRef.type?.extension, dataAuthorityExtension)
    }
}
