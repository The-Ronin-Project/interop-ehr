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
