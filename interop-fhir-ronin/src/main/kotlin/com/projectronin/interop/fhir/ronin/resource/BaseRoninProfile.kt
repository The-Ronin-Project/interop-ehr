package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.ronin.ProfileTransformer
import com.projectronin.interop.fhir.ronin.ProfileValidator
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KLogger

/**
 * Base class capable of handling common tasks associated to Ronin profiles.
 */
abstract class BaseRoninProfile<T : Resource<T>>(protected val logger: KLogger) :
    ProfileValidator<T>, ProfileTransformer<T> {
    /**
     * Internal transformation from [original] based off the [tenant]. This is internal because validation will be handled commonly for all extending classes.
     */
    abstract fun transformInternal(original: T, tenant: Tenant): Pair<T, Validation>

    abstract fun validateInternal(resource: T, validation: Validation)

    override fun validate(resource: T): Validation {
        return validation {
            validateInternal(resource, this)
            merge(resource.validate())
        }
    }

    private fun validate(resource: T, validation: Validation): Validation {
        validation.merge(validate(resource))
        return validation
    }

    override fun transform(original: T, tenant: Tenant): T? {
        val (transformed, validation) = transformInternal(original, tenant)

        return try {
            validate(transformed, validation).alertIfErrors()
            transformed
        } catch (e: Exception) {
            logger.error(e) { "Unable to transform to profile" }
            null
        }
    }

    /**
     * Validates that the supplied [identifier] list contains at least one valid tenant identifier.
     */
    protected fun requireTenantIdentifier(identifier: List<Identifier>, validation: Validation) {
        val tenantIdentifier = identifier.find { it.system == CodeSystem.RONIN_TENANT.uri }
        validation.apply {
            notNull(tenantIdentifier) { "Tenant identifier is required" }
            ifNotNull(tenantIdentifier) {
                // tenantIdentifier.use is constrained by the IdentifierUse enum type, so it needs no validation.
                check(tenantIdentifier.type == CodeableConcepts.RONIN_TENANT) { "Tenant identifier provided without proper CodeableConcept defined" }
                notNull(tenantIdentifier.value) { "tenant identifier value is required" }
            }
        }
    }
}
