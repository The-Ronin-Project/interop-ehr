package com.projectronin.interop.fhir.ronin.resource.diagnosticReport

import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.validate.resource.R4DiagnosticReportValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and Transformer for the Ronin Diagnostic Report Note Exchange profile.
 */
@Component
class RoninDiagnosticReportNoteExchange(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninDiagnosticReport(
        R4DiagnosticReportValidator,
        RoninProfile.DIAGNOSTIC_REPORT_NOTE_EXCHANGE.value,
        normalizer,
        localizer
    ) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    private val requiredCategoryFieldError = RequiredFieldError(DiagnosticReport::category)
    private val requiredSubjectFieldError = RequiredFieldError(DiagnosticReport::subject)
    private val requiredIdError = RequiredFieldError(DiagnosticReport::id)

    override fun validateUSCore(element: DiagnosticReport, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)

        validation.apply {
            checkTrue(element.category.isNotEmpty(), requiredCategoryFieldError, parentContext)

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
