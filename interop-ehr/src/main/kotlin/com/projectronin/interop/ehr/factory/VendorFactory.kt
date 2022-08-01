package com.projectronin.interop.ehr.factory

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.ConditionService
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.MessageService
import com.projectronin.interop.ehr.ObservationService
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.ehr.PractitionerService
import com.projectronin.interop.ehr.model.EHRResource
import kotlin.reflect.KClass

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

    // Util functions for Mirth
    fun <T : EHRResource> deserialize(string: String, type: KClass<T>): EHRResource

    fun <T : EHRResource> deserializeList(string: String, type: KClass<T>): List<EHRResource>

    // this is used by Mirth, so we don't want to use the default "NON_EMPTY" option for serializing
    // since an empty string is a valid non-null option, and on deserializing that'll error if it's missing
    fun <T> serializeObject(t: T): String
}
