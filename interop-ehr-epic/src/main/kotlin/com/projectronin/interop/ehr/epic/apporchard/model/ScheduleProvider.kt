package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Represents an Epic ItemValue from AppOrchard.
 *
 * See [GetProviderAppointments](https://apporchard.epic.com/Sandbox?api=202#1ParamType19155)
 */
@Schema
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class ScheduleProvider(
    @JsonProperty("ID")
    @field:Schema(example = "IPMD", type = "string")
    val id: String,
    @JsonProperty("IDType")
    @field:Schema(example = "External", type = "string")
    val idType: String = "External",
    val departmentID: String = "",
    val departmentIDType: String = "",
)
