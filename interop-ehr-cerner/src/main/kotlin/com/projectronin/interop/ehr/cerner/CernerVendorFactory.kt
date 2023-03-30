package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.OnboardFlagService
import com.projectronin.interop.ehr.OrganizationService
import com.projectronin.interop.ehr.PractitionerRoleService
import com.projectronin.interop.ehr.factory.VendorFactory
import org.springframework.stereotype.Service

/**
 * Cerner implementation of [VendorFactory]
 */
@Service
class CernerVendorFactory(
    override val appointmentService: CernerAppointmentService,
    override val conditionService: CernerConditionService,
    override val encounterService: CernerEncounterService,
    override val healthCheckService: CernerHealthCheckService,
    override val identifierService: CernerIdentifierService,
    override val locationService: CernerLocationService,
    override val medicationStatementService: CernerMedicationStatementService,
    override val messageService: CernerMessageService,
    override val patientService: CernerPatientService,
    override val practitionerService: CernerPractitionerService,
    override val observationService: CernerObservationService,
    override val medicationService: CernerMedicationService,
    override val medicationRequestService: CernerMedicationRequestService
) : VendorFactory {
    override val vendorType: VendorType = VendorType.CERNER

    override val practitionerRoleService: PractitionerRoleService
        get() = TODO("Not yet implemented")
    override val organizationService: OrganizationService
        get() = TODO("Not yet implemented")
    override val onboardFlagService: OnboardFlagService
        get() = TODO("Not yet implemented")
}
