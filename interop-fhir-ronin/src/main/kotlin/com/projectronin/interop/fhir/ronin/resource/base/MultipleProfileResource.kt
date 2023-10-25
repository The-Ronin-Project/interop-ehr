package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDateTime

/**
 * Base class for supporting resources with multiple profiles. Ex: RoninObservations, RoninConditions.
 *
 * When determining the correct profile to use with a resource, the full list of [potentialProfiles] is evaluated
 * to see which of the profiles fits the attributes of the resource. Ex: Observation body weight vs. lab results.
 * If a fit is not found, this class may specify a [defaultProfile] which does not overlap with the [potentialProfiles]
 * and this [defaultProfile] will be applied to any resource that does not qualify for the [potentialProfiles].
 *
 * A [defaultProfile] is not required. If not supplied, or if the resource does not qualify for either the
 * [potentialProfiles] or the [defaultProfile], the resource does not qualify for any profile, and simply errors.
 */
abstract class MultipleProfileResource<T : Resource<T>>(normalizer: Normalizer, localizer: Localizer) :
    BaseProfile<T>(normalizer = normalizer, localizer = localizer) {
    protected abstract val potentialProfiles: List<BaseProfile<T>>
    protected open val defaultProfile: BaseProfile<T>? = null

    override fun validate(element: T, parentContext: LocationContext, validation: Validation) {
        val qualifiedProfile = getQualifiedProfile(element, parentContext, validation)
        qualifiedProfile.forEach {
            validation.merge(it.validate(element, parentContext))
        }
    }

    private val noProfilesError = FHIRError(
        code = "RONIN_PROFILE_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "No profiles qualified",
        location = null
    )
    private val tooManyProfilesError = FHIRError(
        code = "RONIN_PROFILE_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Multiple profiles qualified",
        location = null
    )
    private val noProfilesDefaultWarning = FHIRError(
        code = "RONIN_PROFILE_003",
        severity = ValidationIssueSeverity.WARNING,
        description = "No profiles qualified, the default profile was used",
        location = null
    )

    /**
     * Retrieves the qualified profile for the [resource]. transform() and
     * validate() each call getQualifiedProfile() to select a profile to use.
     *
     * getQualifiedProfile() calls qualifies() on every profile in the
     * MultipleProfileResource. returns all profiles that match,
     * 0 matches selects the default profile and warns; if no default profile,
     * 0 matches is an error. Warnings or errors are added to the [validation].
     */
    private fun getQualifiedProfile(
        resource: T,
        parentContext: LocationContext,
        validation: Validation
    ): List<BaseProfile<T>> {
        val qualifiedProfiles = potentialProfiles.filter { it.qualifies(resource) }
        return if (qualifiedProfiles.isEmpty()) {
            validation.checkTrue(
                false,
                defaultProfile?.let { noProfilesDefaultWarning } ?: noProfilesError,
                parentContext
            )
            defaultProfile?.let { listOf(it) } ?: emptyList()
        } else {
            qualifiedProfiles
        }
    }

    override fun conceptMap(
        normalized: T,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<T?, Validation> {
        val validation = Validation()

        val qualified = getQualifiedProfile(normalized, parentContext, validation)
        val response = qualified.firstOrNull()?.conceptMap(normalized, parentContext, tenant)

        val mapped = response?.let {
            validation.merge(it.second)
            it.first
        }
        return Pair(mapped, validation)
    }

    override fun transformInternal(
        normalized: T,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<T>?, Validation> {
        val validation = Validation()

        val qualified = getQualifiedProfile(normalized, parentContext, validation)
        var transformed = normalized
        val embedded = mutableListOf<Resource<*>>()
        qualified.forEach {
            val response = it?.transformInternal(transformed, parentContext, tenant)
            if (response != null) {
                validation.merge(response.second)
                transformed = response.first?.resource!!
                embedded.addAll(response.first!!.embeddedResources)
            }
        }
        return Pair(TransformResponse(transformed, embedded), validation)
    }
}
