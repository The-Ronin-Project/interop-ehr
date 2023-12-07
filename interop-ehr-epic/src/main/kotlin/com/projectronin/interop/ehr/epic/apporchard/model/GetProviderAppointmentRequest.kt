package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Data class for making requests to Epic's [GetProviderAppointments](https://apporchard.epic.com/Sandbox?api=202) api.
 */
@Schema(description = "Model for making requests to Epic's GetProviderAppointments API")
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class GetProviderAppointmentRequest(
    @field:Schema(example = "IPMD", type = "string")
    val userID: String?,
    @field:Schema(example = "External", type = "string")
    val userIDType: String? = "External",
    @field:Schema(example = "01/01/2022", type = "string")
    val startDate: String?,
    @field:Schema(example = "05/01/2022", type = "string")
    val endDate: String?,
    // This is a Boolean in the AppOrchard spec, but a string in their sample data
    val combineDepartments: String? = "true",
    val resourceType: String? = "",
    val specialty: String? = "",
    val extraItems: List<Any>? = listOf(),
    val providers: List<ScheduleProvider>? = listOf(),
    val departments: List<IDType>? = listOf(),
    val subgroups: List<Any>? = listOf(),
    val extraExtensions: List<Any>? = listOf(),
    val includeAllStatuses: String? = "true",
)
