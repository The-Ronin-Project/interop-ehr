package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.base.BaseProfile
import com.projectronin.interop.fhir.ronin.resource.base.MultipleProfileResource
import com.projectronin.interop.fhir.ronin.resource.diagnosticReport.RoninDiagnosticReportLaboratory
import com.projectronin.interop.fhir.ronin.resource.diagnosticReport.RoninDiagnosticReportNoteExchange
import org.springframework.stereotype.Component

/**
 * Validator and Transformer for the group of active Ronin Diagnostic Reports profiles.
 */
@Component
class RoninDiagnosticReports(
    normalizer: Normalizer,
    localizer: Localizer,
    roninDiagnosticReportLaboratory: RoninDiagnosticReportLaboratory,
    roninDiagnosticReportNoteExchange: RoninDiagnosticReportNoteExchange
) :
    MultipleProfileResource<DiagnosticReport>(normalizer, localizer) {
    override val potentialProfiles: List<BaseProfile<DiagnosticReport>> =
        listOf(roninDiagnosticReportLaboratory, roninDiagnosticReportNoteExchange)
}
