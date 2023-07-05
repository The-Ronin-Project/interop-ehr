package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.ReferenceGenerator
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.util.dataAuthorityIdentifier

/**
 * Generate a reference attribute that conforms to a particular rcdm profile.
 * If both [type] and [id] are input, use [tenantId] to return .reference as
 * [type]/[tenantId]-[id] with .type [type] and the .type.extension for EHRDA.
 * If [type] or [id] are missing and the [reference] is valid, return that.
 * Otherwise generate a reference to one of the [profileAllowedTypes].
 */
fun generateReference(
    reference: Reference,
    profileAllowedTypes: List<String>,
    tenantId: String,
    type: String? = null,
    id: String? = null
): Reference {
    return when {
        type.isNullOrEmpty() || id.isNullOrEmpty() -> when {
            reference.type?.extension == dataAuthorityExtension -> reference
            else -> rcdmReference(
                profileAllowedTypes.random(),
                udpIdValue(tenantId)
            )
        }
        else -> rcdmReference(type, udpIdValue(tenantId, id))
    }
}

/**
 * ronin reference generator that returns the reference with the data authority identifier
 */
fun rcdmReference(type: String, id: String): Reference {
    val reference = ReferenceGenerator()
    reference.reference of "$type/$id".asFHIR()
    reference.type of Uri(
        type,
        extension = dataAuthorityExtension
    )
    return reference.generate()
}

val dataAuthorityExtension = listOf(
    Extension(
        url = RoninExtension.RONIN_DATA_AUTHORITY_EXTENSION.uri,
        value = DynamicValue(
            type = DynamicValueType.IDENTIFIER,
            dataAuthorityIdentifier
        )
    )
)
