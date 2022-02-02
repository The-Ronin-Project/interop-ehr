package com.projectronin.interop.fhir.r4.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier

/**
 * Validates that the supplied [identifier] list contains at least one valid tenant identifier.
 */
fun requireTenantIdentifier(identifier: List<Identifier>) {
    val tenantIdentifier = identifier.find { it.system == CodeSystem.RONIN_TENANT.uri }
    requireNotNull(tenantIdentifier) { "Tenant identifier is required" }

    require(tenantIdentifier.type == CodeableConcepts.RONIN_TENANT) { "Tenant identifier provided without proper CodeableConcept defined" }
    requireNotNull(tenantIdentifier.value) { "tenant value is required" }
}
