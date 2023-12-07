package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.decorator.DecoratorPatientService
import com.projectronin.interop.ehr.factory.VendorFactory
import org.springframework.stereotype.Component

/**
 * Epic implementation of [VendorFactory].
 */
@Component
class EpicVendorFactory(
    override val appointmentService: EpicAppointmentService,
    override val conditionService: EpicConditionService,
    override val encounterService: EpicEncounterService,
    override val healthCheckService: EpicHealthCheckService,
    override val identifierService: EpicIdentifierService,
    override val locationService: EpicLocationService,
    override val medicationService: EpicMedicationService,
    override val medicationStatementService: EpicMedicationStatementService,
    override val medicationRequestService: EpicMedicationRequestService,
    override val messageService: EpicMessageService,
    override val noteService: EpicNoteService,
    override val observationService: EpicObservationService,
    override val onboardFlagService: EpicOnboardFlagService,
    override val organizationService: EpicOrganizationService,
    patientService: EpicPatientService,
    override val practitionerService: EpicPractitionerService,
    override val practitionerRoleService: EpicPractitionerRoleService,
    override val requestGroupService: EpicRequestGroupService,
    override val carePlanService: EpicCarePlanService,
    override val documentReferenceService: EpicDocumentReferenceService,
    override val binaryService: EpicBinaryService,
    override val medicationAdministrationService: EpicMedicationAdministrationService,
    override val serviceRequestService: EpicServiceRequestService,
    override val diagnosticReportService: EpicDiagnosticReportService,
    override val procedureService: EpicProcedureService,
) : VendorFactory {
    override val vendorType: VendorType
        get() = VendorType.EPIC

    override val patientService: PatientService by lazy { DecoratorPatientService(patientService, identifierService) }
}
