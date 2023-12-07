package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.ronin.ProfileQualifier
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.transform.ProfileTransformer
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import java.time.LocalDateTime

abstract class BaseProfile<T : Resource<T>>(
    extendedProfile: ProfileValidator<T>? = null,
    protected val normalizer: Normalizer,
    protected val localizer: Localizer,
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
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime? = null,
    ): Pair<TransformResponse<T>?, Validation>

    /**
     * Access the DataNormalizationRegistry to concept map any Code, Coding, or
     * CodeableConcept attributes in the [normalized] element based off [tenant]
     * prior to any other actions in transform(). Validation reports any errors.
     */
    open fun conceptMap(
        normalized: T,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime? = null,
    ): Pair<T?, Validation> {
        return Pair(normalized, Validation())
    }

    /**
     * Validates the [resource] for the [validation].
     */
    private fun validateTransformation(
        resource: T,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.merge(validate(resource, parentContext))
    }

    override fun transform(
        original: T,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): Pair<TransformResponse<T>?, Validation> {
        val currentContext = LocationContext(original::class)

        val validation =
            validation {
                checkNotNull(
                    original.id,
                    RequiredFieldError(LocationContext(original::class.java.simpleName, "id")),
                    currentContext,
                )
            }

        val normalized = normalizer.normalize(original, tenant)

        val (mapped, mappingValidation) = conceptMap(normalized, currentContext, tenant)
        validation.merge(mappingValidation)

        val (transformResponse, transformValidation) =
            mapped?.let { transformInternal(mapped, currentContext, tenant) }
                ?: Pair(null, null)
        transformValidation?.let { validation.merge(transformValidation) }

        val localized = transformResponse?.let { localizer.localize(transformResponse.resource, tenant) }
        localized?.let { validateTransformation(localized, currentContext, validation) }

        val validatedResponse =
            if (validation.hasErrors()) {
                null
            } else {
                localized?.let { TransformResponse(localized, transformResponse.embeddedResources) }
            }
        return Pair(validatedResponse, validation)
    }
}
