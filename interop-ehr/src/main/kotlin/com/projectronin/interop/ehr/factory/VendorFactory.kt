package com.projectronin.interop.ehr.factory

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.ConditionService
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.LocationService
import com.projectronin.interop.ehr.MedicationService
import com.projectronin.interop.ehr.MedicationStatementService
import com.projectronin.interop.ehr.MessageService
import com.projectronin.interop.ehr.ObservationService
import com.projectronin.interop.ehr.PatientService
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
    val messageService: MessageService
    val patientService: PatientService
    val practitionerService: PractitionerService
    val conditionService: ConditionService
    val identifierService: IdentifierService
    val observationService: ObservationService
    val locationService: LocationService
    val medicationService: MedicationService
    val medicationStatementService: MedicationStatementService
}
