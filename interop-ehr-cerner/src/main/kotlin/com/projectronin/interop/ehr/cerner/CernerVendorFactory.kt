package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.LocationService
import com.projectronin.interop.ehr.MedicationRequestService
import com.projectronin.interop.ehr.MedicationService
import com.projectronin.interop.ehr.MedicationStatementService
import com.projectronin.interop.ehr.MessageService
import com.projectronin.interop.ehr.ObservationService
import com.projectronin.interop.ehr.PractitionerRoleService
import com.projectronin.interop.ehr.factory.VendorFactory
import org.springframework.stereotype.Service

/**
 * Cerner implementation of [VendorFactory]
 */
@Service
class CernerVendorFactory(
    override val patientService: CernerPatientService,
    override val practitionerService: CernerPractitionerService,
    override val appointmentService: CernerAppointmentService,
    override val conditionService: CernerConditionService
) : VendorFactory {
    override val vendorType: VendorType = VendorType.CERNER

    override val messageService: MessageService
        get() = TODO("Not yet implemented")
    override val practitionerRoleService: PractitionerRoleService
        get() = TODO("Not yet implemented")
    override val identifierService: IdentifierService
        get() = TODO("Not yet implemented")
    override val observationService: ObservationService
        get() = TODO("Not yet implemented")
    override val locationService: LocationService
        get() = TODO("Not yet implemented")
    override val medicationService: MedicationService
        get() = TODO("Not yet implemented")
    override val medicationStatementService: MedicationStatementService
        get() = TODO("Not yet implemented")
    override val medicationRequestService: MedicationRequestService
        get() = TODO("Not yet implemented")
}
