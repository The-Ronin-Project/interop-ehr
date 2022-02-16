package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FHIRIdentifiersTest {
    @Test
    fun `can be created`() {
        val identifiers = FHIRIdentifiers(Id("1234"), listOf(Identifier(system = Uri("system"), value = "value")))

        assertEquals(Id("1234"), identifiers.id)
        assertEquals(listOf(Identifier(system = Uri("system"), value = "value")), identifiers.identifiers)
    }
}
