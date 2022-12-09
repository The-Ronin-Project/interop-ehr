package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Base class for supporting resources with multiple profiles.
 */
abstract class MultipleProfileResource<T : Resource<T>> : BaseProfile<T>() {
    protected abstract val potentialProfiles: List<BaseProfile<T>>

    override fun validate(element: T, parentContext: LocationContext, validation: Validation) {
        val qualifiedProfile = getQualifiedProfile(element, parentContext, validation)
        qualifiedProfile?.let { validation.merge(it.validate(element, parentContext)) }
    }

    override fun transformInternal(
        normalized: T,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<T?, Validation> {
        val validation = Validation()

        val qualifiedProfile = getQualifiedProfile(normalized, parentContext, validation)
        val response = qualifiedProfile?.transformInternal(normalized, parentContext, tenant)

        val transformed = response?.let {
            validation.merge(it.second)
            it.first
        }
        return Pair(transformed, validation)
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

    /**
     * Retrieves the qualified profile for the [resource]. If no profiles or multiple profiles qualify, an error will be added to the [validation].
     */
    private fun getQualifiedProfile(
        resource: T,
        parentContext: LocationContext,
        validation: Validation
    ): BaseProfile<T>? {
        val qualifiedProfiles = potentialProfiles.filter { it.qualifies(resource) }
        return when (qualifiedProfiles.size) {
            1 -> qualifiedProfiles[0]
            0 -> {
                validation.checkTrue(false, noProfilesError, parentContext)
                null
            }
            else -> {
                validation.checkTrue(
                    false,
                    tooManyProfilesError,
                    parentContext
                ) { "Multiple profiles qualified: ${qualifiedProfiles.joinToString(", ") { it::class.java.simpleName }}" }
                null
            }
        }
    }
}
