package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.NoteService
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.PractitionerRoleService
import com.projectronin.interop.ehr.decorator.DecoratorPatientService
import com.projectronin.interop.ehr.factory.VendorFactory
import org.springframework.stereotype.Component

/**
 * Cerner implementation of [VendorFactory]
 */
@Component
class CernerVendorFactory(
    override val appointmentService: CernerAppointmentService,
    override val conditionService: CernerConditionService,
    override val encounterService: CernerEncounterService,
    override val healthCheckService: CernerHealthCheckService,
    override val identifierService: CernerIdentifierService,
    override val locationService: CernerLocationService,
    override val medicationStatementService: CernerMedicationStatementService,
    override val messageService: CernerMessageService,
    patientService: CernerPatientService,
    override val practitionerService: CernerPractitionerService,
    override val observationService: CernerObservationService,
    override val organizationService: CernerOrganizationService,
    override val medicationService: CernerMedicationService,
    override val medicationRequestService: CernerMedicationRequestService,
    override val requestGroupService: CernerRequestGroupService,
    override val carePlanService: CernerCarePlanService,
    override val documentReferenceService: CernerDocumentReferenceService,
    override val binaryService: CernerBinaryService,
    override val onboardFlagService: CernerOnboardFlagService,
    override val medicationAdministrationService: CernerMedicationAdministrationService,
    override val serviceRequestService: CernerServiceRequestService,
    override val diagnosticReportService: CernerDiagnosticReportService,
    override val procedureService: CernerProcedureService,
) : VendorFactory {
    override val vendorType: VendorType = VendorType.CERNER

    override val patientService: PatientService by lazy { DecoratorPatientService(patientService, identifierService) }

    override val practitionerRoleService: PractitionerRoleService
        get() = TODO("Not yet implemented")

    override val noteService: NoteService
        get() = TODO("Not yet implemented")
}
