package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocalizersTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `prepends tenant mnemonic to string`() {
        assertEquals("test-some string", "some string".localize(tenant))
    }

    @Test
    fun `Reference with null value is not localized`() {
        val reference = Reference(
            id = FHIRString("id"),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            )
        )

        val localizedReference = reference.localizeReference(tenant)
        assertEquals(null, localizedReference.reference)
    }

    @Test
    fun `non-reference String is not localized`() {
        val reference = Reference(
            reference = FHIRString("http://espn.com")
        )
        val nonReferenceString = reference.localizeReference(tenant)
        assertEquals(FHIRString("http://espn.com"), nonReferenceString.reference)
    }

    @Test
    fun `localizes reference String with full URL and no history`() {
        val reference = Reference(
            reference = FHIRString("http://fhir.hl7.org/svc/StructureDefinition/c8973a22-2b5b-4e76-9c66-00639c99e61b")
        )
        val localize = reference.localizeReference(tenant)
        assertEquals(
            FHIRString("StructureDefinition/test-c8973a22-2b5b-4e76-9c66-00639c99e61b"),
            localize.reference
        )
    }

    @Test
    fun `localizes reference String with full URL and history`() {
        val reference = Reference(
            reference = FHIRString("http://example.org/fhir/Observation/1x2/_history/2")
        )
        val localize = reference.localizeReference(tenant)
        assertEquals(
            FHIRString("Observation/test-1x2/_history/2"),
            localize.reference
        )
    }

    @Test
    fun `localizes reference String with relative URL and no history`() {
        val reference = Reference(
            reference = FHIRString("Patient/034AB16")
        )
        val localize = reference.localizeReference(tenant)
        assertEquals(FHIRString("Patient/test-034AB16"), localize.reference)
    }

    @Test
    fun `localizes reference String with relative URL and history`() {
        val reference = Reference(
            reference = FHIRString("Patient/034AB16/_history/100")
        )
        val localize = reference.localizeReference(tenant)
        assertEquals(
            FHIRString("Patient/test-034AB16/_history/100"),
            localize.reference
        )
    }

    @Test
    fun `non-reference String with id and extensions is not localized`() {
        val reference = Reference(
            id = FHIRString("id"),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            reference = FHIRString("http://espn.com")
        )
        val nonReferenceString = reference.localizeReference(tenant)
        assertEquals(
            FHIRString("http://espn.com"),
            nonReferenceString.reference
        )
        assertEquals(reference.id, nonReferenceString.id)
        assertEquals(reference.extension, nonReferenceString.extension)
    }

    @Test
    fun `localizes reference String with full URL and no history and id and extensions`() {
        val reference = Reference(
            id = FHIRString("id"),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            reference = FHIRString(
                "http://fhir.hl7.org/svc/StructureDefinition/c8973a22-2b5b-4e76-9c66-00639c99e61b"
            )
        )
        val localized = reference.localizeReference(tenant)
        assertEquals(
            FHIRString("StructureDefinition/test-c8973a22-2b5b-4e76-9c66-00639c99e61b"),
            localized.reference
        )
        assertEquals(reference.id, localized.id)
        assertEquals(reference.extension, localized.extension)
        assertEquals(localized.type?.extension, dataAuthorityExtension) // checking just for fun
    }

    @Test
    fun `localized reference includes prior reference id and extensions`() {
        val reference = Reference(
            id = FHIRString("id"),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            reference = FHIRString(
                "Patient/1234",
                FHIRString("id123"),
                listOf(
                    Extension(
                        url = Uri("http://localhost/reference-extension"),
                        value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE)
                    )
                )
            )
        )
        val localized = reference.localizeReference(tenant)
        assertEquals(
            FHIRString(
                "Patient/test-1234",
                FHIRString("id123"),
                listOf(
                    Extension(
                        url = Uri("http://localhost/reference-extension"),
                        value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE)
                    )
                )
            ),
            localized.reference
        )
        assertEquals(reference.id, localized.id)
        assertEquals(reference.extension, localized.extension)
        assertEquals(localized.type?.extension, dataAuthorityExtension) // checking just for fun
    }
}
