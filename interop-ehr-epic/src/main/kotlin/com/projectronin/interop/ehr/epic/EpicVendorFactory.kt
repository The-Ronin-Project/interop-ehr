package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.epic.transform.EpicAppointmentTransformer
import com.projectronin.interop.ehr.factory.VendorFactory
import com.projectronin.interop.transform.fhir.r4.R4LocationTransformer
import com.projectronin.interop.transform.fhir.r4.R4PatientTransformer
import com.projectronin.interop.transform.fhir.r4.R4PractitionerRoleTransformer
import com.projectronin.interop.transform.fhir.r4.R4PractitionerTransformer
import org.springframework.beans.factory.annotation.Qualifier
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
    override val practitionerTransformer: R4PractitionerTransformer,
    override val practitionerRoleTransformer: R4PractitionerRoleTransformer,
    override val locationTransformer: R4LocationTransformer,
    @Qualifier("epic") override val patientTransformer: R4PatientTransformer,
    override val appointmentTransformer: EpicAppointmentTransformer
) : VendorFactory {
    override val vendorType: VendorType
        get() = VendorType.EPIC
}
