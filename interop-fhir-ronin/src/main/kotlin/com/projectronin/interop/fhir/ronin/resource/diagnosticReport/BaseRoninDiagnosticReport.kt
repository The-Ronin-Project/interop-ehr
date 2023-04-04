package com.projectronin.interop.fhir.ronin.resource.diagnosticReport

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.isInValueSet
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity

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
    open val qualifyingCategories: List<Coding> = emptyList()

    override fun qualifies(resource: DiagnosticReport): Boolean {
        return qualifyingCategories.isEmpty() || resource.category.any { category ->
            category.coding.any { it.isInValueSet(qualifyingCategories) }
        }
    }

    private val requiredCodeError = RequiredFieldError(Condition::code)

    override fun validateUSCore(element: DiagnosticReport, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.code, requiredCodeError, parentContext)

            if (element.category.isNotEmpty() && qualifyingCategories.isNotEmpty()) {
                checkTrue(
                    element.category.any { category ->
                        category.coding.any { it.isInValueSet(qualifyingCategories) }
                    },
                    FHIRError(
                        code = "USCORE_DX_RPT_001",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${ qualifyingCategories.joinToString(", ") { "${it.system?.value}|${it.code?.value}" } }",
                        location = LocationContext(DiagnosticReport::category)
                    ),
                    parentContext
                )
            }
        }
    }
}
