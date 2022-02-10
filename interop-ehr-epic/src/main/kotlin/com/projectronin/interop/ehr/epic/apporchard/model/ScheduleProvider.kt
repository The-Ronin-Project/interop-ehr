package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Represents an Epic ItemValue from AppOrchard.
 *
 * See [GetProviderAppointments](https://apporchard.epic.com/Sandbox?api=202#1ParamType19155)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class ScheduleProvider(
    @JsonProperty("ID")
    val id: String,
    @JsonProperty("IDType")
    val idType: String = "External",
    val departmentID: String = "",
    val departmentIDType: String = "",
)
