package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Represents an Appointment for a patient returned from Epic AppOrchard.
 *
 * See [GetPatientAppointments](https://appmarket.epic.com/Sandbox?api=195)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EpicAppointment(
    val appointmentDuration: String = "",
    val appointmentNotes: List<String> = listOf(), // The spec says this is a string, but in their sample data it's an array
    val appointmentStartTime: String = "",
    val appointmentStatus: String = "",
    val contactIDs: List<IDType> = listOf(),
    val date: String = "",
    val patientIDs: List<IDType> = listOf(),
    val patientName: String = "",
    val providers: List<ScheduleProviderReturnWithTime> = listOf(),
    val visitTypeName: String = "",
    var transactionID: String = ""
) {
    @get:JsonIgnore
    val id: String by lazy {
        contactIDs.first { it.type == "CSN" }.id
    }

    // Patient id is pulled from the "Internal" id.
    // [Data Platform](https://github.com/projectronin/dp-databricks-jobs/blob/513cd599955f5905dd20623b2714de2ab4d9c3c0/jobs/gold/mdaoc/fhir/appointment.py#L151).
    @get:JsonIgnore
    val patientId: String? by lazy {
        patientIDs.firstOrNull { it.type == "Internal" }?.id
    }
}
