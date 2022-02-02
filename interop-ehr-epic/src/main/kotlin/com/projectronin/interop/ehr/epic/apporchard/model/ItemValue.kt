package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Represents an Epic ItemValue from AppOrchard.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195#1ParamType18981)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class ItemValue(
    val itemNumber: String,
    val lines: List<LineValue> = listOf(),
    val value: String
)
