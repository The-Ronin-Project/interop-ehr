package com.projectronin.interop.ehr.factory

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.ConditionService
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.MessageService
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.PractitionerService
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.transform.AppointmentTransformer
import com.projectronin.interop.ehr.transform.ConditionTransformer
import com.projectronin.interop.ehr.transform.LocationTransformer
import com.projectronin.interop.ehr.transform.PatientTransformer
import com.projectronin.interop.ehr.transform.PractitionerRoleTransformer
import com.projectronin.interop.ehr.transform.PractitionerTransformer

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

    // Transformers
    val practitionerTransformer: PractitionerTransformer
    val practitionerRoleTransformer: PractitionerRoleTransformer
    val locationTransformer: LocationTransformer
    val patientTransformer: PatientTransformer
    val appointmentTransformer: AppointmentTransformer
    val conditionTransformer: ConditionTransformer

    // Util functions for Mirth
    fun deserializeAppointments(string: String): List<Appointment>
    fun <T> serializeObject(t: T): String
}
