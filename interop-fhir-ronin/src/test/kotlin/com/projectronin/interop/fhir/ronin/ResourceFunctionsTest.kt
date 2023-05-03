package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ResourceFunctionsTest {
    @Test
    fun `resource fhir identifiers handles no id`() {
        val resource = SampleResource()
        val identifiers = resource.getFhirIdentifiers()
        assertEquals(listOf<Identifier>(), identifiers)
    }

    @Test
    fun `resource fhir identifiers handles id`() {
        val resource = SampleResource(id = Id("1234"))
        val identifiers = resource.getFhirIdentifiers()
        assertEquals(
            listOf(
                Identifier(
                    value = "1234".asFHIR(),
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    type = CodeableConcepts.RONIN_FHIR_ID
                )
            ),
            identifiers
        )
    }

    @Test
    fun `id fhir identifier works on null`() {
        val id: Id? = null
        val identifier = id.toFhirIdentifier()
        assertNull(identifier)
    }

    @Test
    fun `id fhir identifier works on non-null`() {
        val identifier = Id("1234").toFhirIdentifier()
        assertEquals(
            Identifier(
                value = "1234".asFHIR(),
                system = CodeSystem.RONIN_FHIR_ID.uri,
                type = CodeableConcepts.RONIN_FHIR_ID
            ),
            identifier
        )
    }

    @Test
    fun `id fhir identifier works on null value`() {
        val identifier = Id(null).toFhirIdentifier()
        assertEquals(
            Identifier(
                value = null,
                system = CodeSystem.RONIN_FHIR_ID.uri,
                type = CodeableConcepts.RONIN_FHIR_ID
            ),
            identifier
        )
    }

    private data class SampleResource(
        override val id: Id? = null,
        override val implicitRules: Uri? = null,
        override val language: Code? = null,
        override var meta: Meta? = null,
        override val resourceType: String = "Sample"
    ) : Resource<SampleResource>
}
