package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Converts this tenant into the appropriate FHIR [Identifier]
 */
fun Tenant.toFhirIdentifier() =
    Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = this.mnemonic)
