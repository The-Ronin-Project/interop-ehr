package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.ronin.ProfileQualifier
import com.projectronin.interop.fhir.ronin.ProfileTransformer
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging

abstract class BaseProfile<T : Resource<T>>(
    extendedProfile: ProfileValidator<T>? = null
) : BaseValidator<T>(extendedProfile), ProfileTransformer<T>, ProfileQualifier<T> {
    protected val logger = KotlinLogging.logger(this::class.java.name)

    override fun qualifies(resource: T): Boolean {
        return true
    }

    /**
     * Internal transformation from [normalized] element based off the [tenant]. This is internal because tenant-transformation and validation will be handled commonly for all extending classes.
     */
    abstract fun transformInternal(
        normalized: T,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<T?, Validation>

    /**
     * Validates the [resource] for the [validation].
     */
    private fun validateTransformation(
        resource: T,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.merge(validate(resource, parentContext))
    }

    override fun transform(original: T, tenant: Tenant): Pair<T?, Validation> {
        val currentContext = LocationContext(original::class)

        val validation = validation {
            checkNotNull(
                original.id,
                RequiredFieldError(LocationContext(original::class.java.simpleName, "id")),
                currentContext
            )
        }

        val normalized = Normalizer.normalize(original, tenant)

        val (transformed, transformValidation) = transformInternal(normalized, currentContext, tenant)
        validation.merge(transformValidation)

        val localizedTransform = transformed?.let { Localizer.localize(transformed, tenant) }
        localizedTransform?.let {
            validateTransformation(it, currentContext, validation)
        }

        val successfullyTransformed = if (validation.hasErrors()) null else localizedTransform
        return Pair(successfullyTransformed, validation)
    }
}
