package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Represents appointments for a patient returned from Epic AppOrchard.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class GetPatientAppointmentsResponse(
    val appointments: List<Appointment>,
    val error: String?
)
