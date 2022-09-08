package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Data class for making requests to Epic's [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195) api.
 */
@Schema(description = "Model for making requests to Epic's GetPatientAppointments API")
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class GetPatientAppointmentsRequest(
    @field:Schema(example = "IPMD", type = "string")
    val userID: String?,
    @field:Schema(example = "01/01/2022", type = "string")
    val startDate: String?,
    @field:Schema(example = "05/01/2022", type = "string")
    val endDate: String?,
    @field:Schema(example = "202497", type = "string")
    val patientId: String?,
    @field:Schema(example = "mrn", type = "string")
    val patientIdType: String? = "MRN",
    @field:Schema(example = "External", type = "string")
    val userIDType: String? = "External",
    @field:Schema(example = "true", type = "string")
    val includeAllStatuses: String? = "true",
)
