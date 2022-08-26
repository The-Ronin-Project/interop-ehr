package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validates this resource against the supplied [validator].
 */
fun <T : Resource<T>> T.validateAs(validator: ProfileValidator<T>): Validation = validator.validate(this)

/**
 * Transforms this resource according to the [transformer] and [tenant].
 */
fun <T : Resource<T>> T.transformTo(transformer: ProfileTransformer<T>, tenant: Tenant) =
    transformer.transform(this, tenant)
