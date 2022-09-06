package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
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
        value = value,
        system = RoninCodeSystem.FHIR_ID.uri,
        type = RoninCodeableConcepts.FHIR_ID
    )
}
