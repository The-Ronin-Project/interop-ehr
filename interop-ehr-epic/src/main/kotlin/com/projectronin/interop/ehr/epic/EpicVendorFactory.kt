package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.factory.VendorFactory
import org.springframework.stereotype.Component

/**
 * Epic implementation of [VendorFactory].
 */
@Component
class EpicVendorFactory(
    override val patientService: EpicPatientService,
    override val appointmentService: EpicAppointmentService,
    override val messageService: EpicMessageService,
    override val practitionerService: EpicPractitionerService,
    override val conditionService: EpicConditionService,
    override val identifierService: EpicIdentifierService,
    override val observationService: EpicObservationService,
    override val locationService: EpicLocationService,
    override val medicationService: EpicMedicationService

) : VendorFactory {
    override val vendorType: VendorType
        get() = VendorType.EPIC
}
