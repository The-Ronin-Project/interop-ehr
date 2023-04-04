package com.projectronin.interop.fhir.ronin.resource.diagnosticReport

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.validate.resource.R4DiagnosticReportValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Validator and Transformer for the Ronin Diagnostic Report Laboratory profile.
 */
@Component
class RoninDiagnosticReportLaboratory(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninDiagnosticReport(
        R4DiagnosticReportValidator,
        RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value,
        normalizer,
        localizer
    ) {

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    override val qualifyingCategories = listOf(Coding(system = CodeSystem.DIAGNOSTIC_REPORT_LABORATORY.uri, code = Code("LAB")))

    private val requiredIdError = RequiredFieldError(DiagnosticReport::id)
    private val requiredSubjectFieldError = RequiredFieldError(DiagnosticReport::subject)
    private val requiredCategoryFieldError = RequiredFieldError(DiagnosticReport::category)

    override fun validateRonin(element: DiagnosticReport, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
        }
    }

    override fun validateUSCore(element: DiagnosticReport, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)

        validation.apply {
            checkTrue(element.category.isNotEmpty(), requiredCategoryFieldError, parentContext)

            checkNotNull(element.subject, requiredSubjectFieldError, parentContext)

            // code and status field checks are done by R4DiagnosticReportValidator
        }
    }

    override fun transformInternal(
        normalized: DiagnosticReport,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<DiagnosticReport?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )

        return Pair(transformed, validation)
    }
}
