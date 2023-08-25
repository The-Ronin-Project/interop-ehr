package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.ExtensionGenerator
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.test.data.generator.collection.ListDataGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninExtensionUtilTest {
    var extension = ListDataGenerator(0, ExtensionGenerator())
    private val providedExtension = listOf(
        Extension(
            url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
            value = DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    text = "something-to-test".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("tenant-source-code-test-extension")
                        )
                    )
                )
            )
        )
    )

    @Test
    fun `include rcdm extension when none is provided`() {
        val roninEx = includeExtensions(extension.generate(), tenantSourceConditionExtension)
        assertEquals(tenantSourceConditionExtension, roninEx)
    }

    @Test
    fun `include rcdm extensions when provided extension`() {
        val roninEx = includeExtensions(providedExtension, tenantSourceConditionExtension)
        assertEquals(providedExtension + tenantSourceConditionExtension, roninEx)
    }

    @Test
    fun `does not include extra rcdm extensions when already provided`() {
        val roninEx = includeExtensions(tenantSourceConditionExtension, tenantSourceConditionExtension)
        assertEquals(tenantSourceConditionExtension, roninEx)
    }
}
