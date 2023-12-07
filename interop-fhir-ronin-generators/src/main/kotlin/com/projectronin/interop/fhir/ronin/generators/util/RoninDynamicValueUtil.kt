package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.DynamicValues
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Reference

/**
 * For a required DynamicValue reference that may reference only certain types
 */
fun generateDynamicValueReference(
    dynamicValue: DynamicValue<Any>,
    profileAllowedTypes: List<String>,
    tenantId: String,
    type: String? = null,
    id: String? = null,
): DynamicValue<Reference> {
    return DynamicValues.reference(
        when {
            dynamicValue.type == DynamicValueType.REFERENCE ->
                generateReference(
                    dynamicValue.value as Reference,
                    profileAllowedTypes,
                    tenantId,
                    type,
                    id,
                )
            else ->
                rcdmReference(
                    profileAllowedTypes.random(),
                    udpIdValue(tenantId, id),
                )
        },
    )
}

/**
 * For an optional DynamicValue reference that may reference only certain types
 */
fun generateOptionalDynamicValueReference(
    dynamicValue: DynamicValue<Any>? = null,
    profileAllowedTypes: List<String>,
    tenantId: String,
    type: String? = null,
    id: String? = null,
): DynamicValue<Any>? {
    return when {
        dynamicValue?.type == DynamicValueType.REFERENCE &&
            (dynamicValue.value as Reference).type?.extension == dataAuthorityExtension ->
            dynamicValue
        else ->
            generateOptionalReference(
                null,
                profileAllowedTypes,
                tenantId,
                type,
                id,
            ).let { if (it == null) null else DynamicValues.reference(it) }
    }
}
