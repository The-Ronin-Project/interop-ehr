package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus

/**
 * Represents an Appointment for a patient returned from Epic AppOrchard.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class Appointment(
    val appointmentDuration: String,
    val appointmentNotes: List<String> = listOf(), // The spec says this is a string, but in their sample data it's an array
    val appointmentStartTime: String,
    val appointmentStatus: String,
    val contactIDs: List<IDType> = listOf(),
    val date: String,
    val extraExtensions: List<ExtensionInformationReturn> = listOf(),
    val extraItems: List<ItemValue>? = listOf(), // They sometimes send "null" instead of an empty list
    val patientIDs: List<IDType> = listOf(),
    val patientName: String,
    val providers: List<ScheduleProviderReturnWithTime> = listOf(),
    val visitTypeIDs: List<IDType> = listOf(),
    val visitTypeName: String
) {
    @get:JsonIgnore
    val id: String by lazy {
        contactIDs.first { it.type == "ASN" }.id
    }

    // Patient id is pulled from the "Internal" id.
    // [Data Platform](https://github.com/projectronin/dp-databricks-jobs/blob/513cd599955f5905dd20623b2714de2ab4d9c3c0/jobs/gold/mdaoc/fhir/appointment.py#L151).
    @get:JsonIgnore
    val patientId: String? by lazy {
        patientIDs.firstOrNull { it.type == "Internal" }?.id
    }

    // Default to entered-in-error to agree with [Data Platform](https://github.com/projectronin/dp-databricks-jobs/blob/513cd599955f5905dd20623b2714de2ab4d9c3c0/jobs/gold/mdaoc/fhir/appointment.py#L114)
    @get:JsonIgnore
    val status: AppointmentStatus by lazy {
        when (appointmentStatus.lowercase()) {
            "completed" -> AppointmentStatus.FULFILLED
            "scheduled" -> AppointmentStatus.PENDING
            "no show" -> AppointmentStatus.NOSHOW
            "arrived" -> AppointmentStatus.ARRIVED
            else -> AppointmentStatus.ENTERED_IN_ERROR
        }
    }
}
