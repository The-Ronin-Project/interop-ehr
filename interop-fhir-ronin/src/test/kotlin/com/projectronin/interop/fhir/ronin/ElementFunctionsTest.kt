package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.element.Element
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ElementFunctionsTest {
    @Test
    fun `no data absent reason in element`() {
        val element = SampleElement()
        assertFalse(element.hasDataAbsentReason())
    }

    @Test
    fun `null element, no data absent reason`() {
        val element: SampleElement? = null
        assertFalse(element.hasDataAbsentReason())
    }

    @Test
    fun `null list, no data absent reason`() {
        val element: List<SampleElement>? = null
        assertFalse(element.hasDataAbsentReason())
    }

    @Test
    fun `data absent reason`() {
        val element =
            SampleElement(
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://hl7.org/fhir/StructureDefinition/data-absent-reason"),
                            value = DynamicValue(DynamicValueType.CODE, Code(value = "asked-declined")),
                        ),
                    ),
            )
        assertTrue(element.hasDataAbsentReason())
    }

    @Test
    fun `data absent reason in list`() {
        val element =
            SampleElement(
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://hl7.org/fhir/StructureDefinition/data-absent-reason"),
                            value = DynamicValue(DynamicValueType.CODE, Code(value = "asked-declined")),
                        ),
                    ),
            )
        assertTrue(listOf(element).hasDataAbsentReason())
    }

    @Test
    fun `no data absent reason in list`() {
        val element = SampleElement()
        assertFalse(listOf(element).hasDataAbsentReason())
    }
}

private data class SampleElement(
    override val extension: List<Extension> = emptyList(),
    override val id: FHIRString? = null,
) : Element<SampleElement>
