package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.ehr.epic.apporchard.model.exceptions.AppOrchardError

/**
 * Represents appointments for a patient or provider returned from Epic AppOrchard.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195)
 * and [GetProviderAppointments](https://apporchard.epic.com/Sandbox?api=202)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class GetAppointmentsResponse(
    val appointments: List<EpicAppointment>?,
    val error: String?
) {
    fun errorOrAppointments(): List<EpicAppointment> = appointments ?: throw AppOrchardError(error)
}
