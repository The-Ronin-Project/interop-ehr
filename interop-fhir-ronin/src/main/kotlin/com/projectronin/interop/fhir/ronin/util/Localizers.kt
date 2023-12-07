package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.tenant.config.model.Tenant

// Localizers for Strings and String-like wrappers

/**
 * Localizes the String relative to the [tenant]
 */
fun String.localize(tenant: Tenant): String {
    val prefix = "${tenant.mnemonic}-"
    return if (this.startsWith(prefix)) this else "$prefix$this"
}

/**
 * Localizes the [reference](http://hl7.org/fhir/R4/references.html) contained by this String relative to the [tenant].
 * If this String does not represent a reference, the original String will be returned. Also returns the reference type.
 */
fun Reference.localizeReference(tenant: Tenant): Reference {
    reference?.value?.let {
        val matchResult = Reference.FHIR_RESOURCE_REGEX.matchEntire(it) ?: return this

        // Should we localize if there's a history?
        val (_, _, _, type, fhirId, history) = matchResult.destructured
        return copy(
            reference =
                FHIRString(
                    "$type/${fhirId.localize(tenant)}$history",
                    reference?.id,
                    reference?.extension ?: listOf(),
                ),
            type = Uri(type, extension = dataAuthorityExtension),
        )
    } ?: return this
}
