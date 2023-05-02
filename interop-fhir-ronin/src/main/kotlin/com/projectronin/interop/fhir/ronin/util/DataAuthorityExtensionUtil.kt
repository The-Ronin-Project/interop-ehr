package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR

/**
 * Data Authority Identifier.
 */
val dataAuthorityIdentifier = Identifier(
    value = "EHR Data Authority".asFHIR(),
    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID
)

/**
 * Data Authority Extension.
 */
val dataAuthorityExtension = listOf(
    Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
        value = DynamicValue(
            type = DynamicValueType.IDENTIFIER,
            dataAuthorityIdentifier
        )
    )
)
