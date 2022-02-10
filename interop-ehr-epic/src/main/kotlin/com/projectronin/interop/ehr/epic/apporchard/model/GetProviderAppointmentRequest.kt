package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Data class for making requests to Epic's [GetProviderAppointments](https://apporchard.epic.com/Sandbox?api=202) api.
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class GetProviderAppointmentRequest(
    val userID: String,
    val userIDType: String = "External",
    val startDate: String,
    val endDate: String,
    val combineDepartments: String = "true", // This is a Boolean in the AppOrchard spec, but a string in their sample data
    val resourceType: String = "",
    val specialty: String = "",
    val extraItems: List<Any> = listOf(),
    val providers: List<ScheduleProvider>,
    val departments: List<Any> = listOf(),
    val subgroups: List<Any> = listOf(),
    val extraExtensions: List<Any> = listOf(),
)
