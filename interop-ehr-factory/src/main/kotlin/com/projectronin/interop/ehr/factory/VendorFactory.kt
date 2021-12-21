package com.projectronin.interop.ehr.factory

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.MessageService
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.PractitionerService
import com.projectronin.interop.transform.PractitionerRoleTransformer
import com.projectronin.interop.transform.PractitionerTransformer

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

    // Transformers
    val practitionerTransformer: PractitionerTransformer
    val practitionerRoleTransformer: PractitionerRoleTransformer
}
