package com.projectronin.interop.fhir.ronin.resource.diagnosticReport

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.qualifiesForValueSet
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDateTime

/**
 * Base class capable of handling common tasks associated to Ronin Condition profiles.
 */
abstract class BaseRoninDiagnosticReport(
    extendedProfile: ProfileValidator<DiagnosticReport>,
    profile: String,
    normalizer: Normalizer,
    localizer: Localizer
) : USCoreBasedProfile<DiagnosticReport>(extendedProfile, profile, normalizer, localizer) {

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    open fun qualifyingCategories(): List<Coding> = emptyList()

    override fun qualifies(resource: DiagnosticReport): Boolean {
        return resource.category.qualifiesForValueSet(qualifyingCategories())
    }

    override fun validateRonin(element: DiagnosticReport, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)
            // check that subject reference extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(DiagnosticReport::subject),
                    validation
                )
            }
        }
    }

    private val requiredCodeError = RequiredFieldError(Condition::code)

    override fun validateUSCore(element: DiagnosticReport, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.code, requiredCodeError, parentContext)
            validateReference(
                element.subject,
                listOf(ResourceType.Patient),
                parentContext.append(LocationContext(DiagnosticReport::subject)),
                this
            )
        }
    }

    private val requiredIdError = RequiredFieldError(DiagnosticReport::id)

    override fun transformInternal(
        normalized: DiagnosticReport,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<DiagnosticReport>?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )

        return Pair(TransformResponse(transformed), validation)
    }
}
