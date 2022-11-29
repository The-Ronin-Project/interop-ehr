package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
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
    fun `FHIRString with null value is not localized`() {
        val id = FHIRString("id")
        val extensions = listOf(
            Extension(
                url = Uri("http://localhost/extension"),
                value = DynamicValue(DynamicValueType.STRING, "Value")
            )
        )
        val string = FHIRString(null, id, extensions)
        assertEquals(string, string.localizeReference(tenant))
    }

    @Test
    fun `non-reference String is not localized`() {
        assertEquals(FHIRString("http://espn.com"), FHIRString("http://espn.com").localizeReference(tenant))
    }

    @Test
    fun `localizes reference String with full URL and no history`() {
        assertEquals(
            FHIRString("StructureDefinition/test-c8973a22-2b5b-4e76-9c66-00639c99e61b"),
            FHIRString("http://fhir.hl7.org/svc/StructureDefinition/c8973a22-2b5b-4e76-9c66-00639c99e61b").localizeReference(
                tenant
            )
        )
    }

    @Test
    fun `localizes reference String with full URL and history`() {
        assertEquals(
            FHIRString("Observation/test-1x2/_history/2"),
            FHIRString("http://example.org/fhir/Observation/1x2/_history/2").localizeReference(tenant)
        )
    }

    @Test
    fun `localizes reference String with relative URL and no history`() {
        assertEquals(FHIRString("Patient/test-034AB16"), FHIRString("Patient/034AB16").localizeReference(tenant))
    }

    @Test
    fun `localizes reference String with relative URL and history`() {
        assertEquals(
            FHIRString("Patient/test-034AB16/_history/100"),
            FHIRString("Patient/034AB16/_history/100").localizeReference(tenant)
        )
    }

    @Test
    fun `non-reference String with id and extensions is not localized`() {
        val id = FHIRString("id")
        val extensions = listOf(
            Extension(
                url = Uri("http://localhost/extension"),
                value = DynamicValue(DynamicValueType.STRING, "Value")
            )
        )
        assertEquals(
            FHIRString("http://espn.com", id, extensions),
            FHIRString("http://espn.com", id, extensions).localizeReference(tenant)
        )
    }

    @Test
    fun `localizes reference String with full URL and no history and id and extensions`() {
        val id = FHIRString("id")
        val extensions = listOf(
            Extension(
                url = Uri("http://localhost/extension"),
                value = DynamicValue(DynamicValueType.STRING, "Value")
            )
        )
        assertEquals(
            FHIRString("StructureDefinition/test-c8973a22-2b5b-4e76-9c66-00639c99e61b", id, extensions),
            FHIRString(
                "http://fhir.hl7.org/svc/StructureDefinition/c8973a22-2b5b-4e76-9c66-00639c99e61b",
                id,
                extensions
            ).localizeReference(
                tenant
            )
        )
    }
}
