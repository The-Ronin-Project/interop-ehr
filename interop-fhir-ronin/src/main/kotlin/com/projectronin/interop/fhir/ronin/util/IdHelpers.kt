package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Converts this tenant into the appropriate FHIR [Identifier]
 */
fun Tenant.toFhirIdentifier() =
    Identifier(
        type = CodeableConcepts.RONIN_TENANT,
        system = CodeSystem.RONIN_TENANT.uri,
        value = FHIRString(mnemonic),
    )
