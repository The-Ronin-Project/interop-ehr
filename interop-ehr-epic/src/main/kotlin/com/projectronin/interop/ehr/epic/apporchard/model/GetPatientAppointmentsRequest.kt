package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Data class for making requests to Epic's [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195) api.
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class GetPatientAppointmentsRequest(
    val userID: String?,
    val startDate: String?,
    val endDate: String?,
    val patientId: String?,
    val patientIdType: String? = "MRN",
    val userIDType: String? = "External"
)
