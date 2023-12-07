package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.ReferenceGenerator
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.ronin.util.dataAuthorityIdentifier
import java.lang.IllegalArgumentException

/**
 * Generate a reference attribute that conforms to a particular rcdm profile.
 * If both [type] and [id] are input, use [tenantId] to return .reference as
 * [type]/[tenantId]-[id] with .type [type] and the .type.extension for EHRDA.
 * If [type] or [id] are missing and [reference] is valid, return that. If [type]
 * is non-empty and not in [profileAllowedTypes] throw IllegalArgumentException.
 * Else: generate reference to one of the [profileAllowedTypes],
 */
fun generateReference(
    reference: Reference? = null,
    profileAllowedTypes: List<String>,
    tenantId: String,
    type: String? = null,
    id: String? = null,
): Reference {
    return when {
        (type.isNullOrEmpty() || id.isNullOrEmpty()) &&
            reference?.type?.extension == dataAuthorityExtension -> reference
        else ->
            generateOptionalReference(
                reference,
                profileAllowedTypes,
                tenantId,
                type,
                id,
            ) ?: rcdmReference(
                profileAllowedTypes.random(),
                udpIdValue(tenantId, id),
            )
    }
}

/**
 * For an optional reference that, if present, may reference only certain types:
 * If the [reference] is valid, return that. Else: if [type] is one of the
 * [profileAllowedTypes], generate a reference to [type] using the input [id] or
 * by generating an id. If [type] is empty or null return null. If [type] is
 * non-empty and not in [profileAllowedTypes] throw IllegalArgumentException.
 */
fun generateOptionalReference(
    reference: Reference? = null,
    profileAllowedTypes: List<String>,
    tenantId: String,
    type: String? = null,
    id: String? = null,
): Reference? {
    return when {
        reference?.type?.extension == dataAuthorityExtension -> reference
        !type.isNullOrEmpty() ->
            when {
                profileAllowedTypes.isEmpty() || type in profileAllowedTypes ->
                    rcdmReference(
                        type,
                        udpIdValue(tenantId, id),
                    )
                else ->
                    throw IllegalArgumentException(
                        "${ if (type.isNullOrEmpty()) "The type provided" else type } is not ${
                            if (profileAllowedTypes.size > 1) "one of " else ""
                        }${profileAllowedTypes.joinToString(", ")}",
                    )
            }
        else -> null
    }
}

/**
 * ronin reference generator that returns the reference with the data authority identifier
 */
fun rcdmReference(
    type: String,
    id: String,
): Reference {
    val reference = ReferenceGenerator()
    reference.reference of "$type/$id".asFHIR()
    reference.type of
        Uri(
            type,
            extension = dataAuthorityExtension,
        )
    return reference.generate()
}

val dataAuthorityExtension =
    listOf(
        Extension(
            url = RoninExtension.RONIN_DATA_AUTHORITY_EXTENSION.uri,
            value =
                DynamicValue(
                    type = DynamicValueType.IDENTIFIER,
                    dataAuthorityIdentifier,
                ),
        ),
    )
