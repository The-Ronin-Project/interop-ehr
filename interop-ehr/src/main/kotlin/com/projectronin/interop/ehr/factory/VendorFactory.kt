package com.projectronin.interop.ehr.factory

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.ConditionService
import com.projectronin.interop.ehr.EncounterService
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.LocationService
import com.projectronin.interop.ehr.MedicationRequestService
import com.projectronin.interop.ehr.MedicationService
import com.projectronin.interop.ehr.MedicationStatementService
import com.projectronin.interop.ehr.MessageService
import com.projectronin.interop.ehr.ObservationService
import com.projectronin.interop.ehr.OnboardFlagService
import com.projectronin.interop.ehr.OrganizationService
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.PractitionerRoleService
import com.projectronin.interop.ehr.PractitionerService

/**
 * Interface defining a factory capable of handling all EHR service implementations for a specific vendor.
 */
interface VendorFactory {
    /**
     * The type of vendor supported by this factory.
     */
    val vendorType: VendorType

    // Retrieval Services
    val appointmentService: AppointmentService
    val conditionService: ConditionService
    val encounterService: EncounterService
    val identifierService: IdentifierService
    val locationService: LocationService
    val observationService: ObservationService
    val onboardFlagService: OnboardFlagService
    val organizationService: OrganizationService
    val medicationService: MedicationService
    val medicationStatementService: MedicationStatementService
    val medicationRequestService: MedicationRequestService
    val messageService: MessageService
    val patientService: PatientService
    val practitionerService: PractitionerService
    val practitionerRoleService: PractitionerRoleService
}
