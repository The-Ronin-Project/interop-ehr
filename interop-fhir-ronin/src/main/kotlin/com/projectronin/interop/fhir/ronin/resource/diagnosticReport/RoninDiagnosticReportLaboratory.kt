package com.projectronin.interop.fhir.ronin.resource.diagnosticReport

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.validate.resource.R4DiagnosticReportValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.qualifiesForValueSet
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

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
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    override val qualifyingCategories =
        listOf(Coding(system = CodeSystem.DIAGNOSTIC_REPORT_LABORATORY.uri, code = Code("LAB")))

    private val requiredIdError = RequiredFieldError(DiagnosticReport::id)
    private val requiredSubjectFieldError = RequiredFieldError(DiagnosticReport::subject)
    private val requiredCategoryFieldError = RequiredFieldError(DiagnosticReport::category)

    override fun validateUSCore(element: DiagnosticReport, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)

        validation.apply {
            checkTrue(element.category.isNotEmpty(), requiredCategoryFieldError, parentContext)
            checkTrue(
                element.category.qualifiesForValueSet(qualifyingCategories),
                FHIRError(
                    code = "USCORE_DX_RPT_001",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Must match this system|code: ${
                    qualifyingCategories.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                    }",
                    location = LocationContext(DiagnosticReport::category)
                ),
                parentContext
            )

            checkNotNull(element.subject, requiredSubjectFieldError, parentContext)
            validateReference(
                element.subject,
                listOf("Patient"),
                LocationContext(DiagnosticReport::subject),
                validation
            )

            // code and status field checks are done by R4DiagnosticReportValidator
        }
    }

    override fun transformInternal(
        normalized: DiagnosticReport,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<DiagnosticReport?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )

        return Pair(transformed, validation)
    }
}
