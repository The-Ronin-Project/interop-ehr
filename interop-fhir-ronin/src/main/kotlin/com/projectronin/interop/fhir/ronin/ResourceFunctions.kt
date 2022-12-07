package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Transforms this resource according to the [transformer] and [tenant].
 */
fun <T : Resource<T>> T.transformTo(transformer: ProfileTransformer<T>, tenant: Tenant) =
    transformer.transform(this, tenant)

fun <T : Resource<T>> T.getFhirIdentifiers(): List<Identifier> =
    id?.toFhirIdentifier()?.let { listOf(it) } ?: emptyList()

fun Id?.toFhirIdentifier(): Identifier? = this?.let {
    Identifier(
        value = value?.let { FHIRString(it) },
        system = CodeSystem.RONIN_FHIR_ID.uri,
        type = CodeableConcepts.RONIN_FHIR_ID
    )
}
