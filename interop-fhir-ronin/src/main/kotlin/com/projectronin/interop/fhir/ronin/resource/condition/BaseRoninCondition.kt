package com.projectronin.interop.fhir.ronin.resource.condition

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.qualifiesForValueSet
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity

/**
 * Base class capable of handling common tasks associated to Ronin Condition profiles.
 */
abstract class BaseRoninCondition(
    extendedProfile: ProfileValidator<Condition>,
    profile: String,
    normalizer: Normalizer,
    localizer: Localizer
) : USCoreBasedProfile<Condition>(extendedProfile, profile, normalizer, localizer) {

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    open val qualifyingCategories: List<Coding> = emptyList()

    override fun qualifies(resource: Condition): Boolean {
        return resource.category.qualifiesForValueSet(qualifyingCategories)
    }

    private val requiredCodeError = RequiredFieldError(Condition::code)

    private val requiredConditionExtension = RequiredFieldError(Condition::extension)
    private val requiredConditionCodeExtension = FHIRError(
        code = "RONIN_CND_001",
        description = "Condition extension requires code extension",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Condition::extension)
    )

    override fun validateRonin(element: Condition, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, validation)
            containedResourcePresent(element.contained, parentContext, validation)

            // check that subject reference has type and the extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(Condition::subject),
                    validation
                )
            }

            checkTrue(
                element.category.qualifiesForValueSet(qualifyingCategories),
                FHIRError(
                    code = "RONIN_CND_001",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Must match this system|code: ${
                    qualifyingCategories.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                    }",
                    location = LocationContext(Condition::category)
                ),
                parentContext
            )

            checkNotNull(element.code, requiredCodeError, parentContext)
            requireCodeableConcept("code", element.code, parentContext, validation)
            // code value set is not validated, as it is too large

            // clinicalStatus required value set is validated in R4
            // verificationStatus required value set is validated in R4

            // subject required is validated in R4
            validateReference(element.subject, listOf("Patient"), LocationContext(Condition::subject), validation)

            checkNotNull(element.extension, requiredConditionExtension, parentContext)
            checkTrue(
                element.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri
                },
                requiredConditionCodeExtension,
                parentContext
            )
        }
    }

    override fun validateUSCore(element: Condition, parentContext: LocationContext, validation: Validation) {}
}
