package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.tenant.config.model.Tenant

// Localizers for Strings and String-like wrappers

/**
 * Localizes the String relative to the [tenant]
 */
fun String.localize(tenant: Tenant) = "${tenant.mnemonic}-$this"

/**
 * Localizes the [reference](http://hl7.org/fhir/R4/references.html) contained by this String relative to the [tenant].
 * If this String does not represent a reference, the original String will be returned.
 */
fun String.localizeReference(tenant: Tenant): String {
    val matchResult = Reference.FHIR_RESOURCE_REGEX.matchEntire(this) ?: return this

    // Should we localize if there's a history?
    val (_, _, _, type, id, history) = matchResult.destructured
    return "$type/${id.localize(tenant)}$history"
}
