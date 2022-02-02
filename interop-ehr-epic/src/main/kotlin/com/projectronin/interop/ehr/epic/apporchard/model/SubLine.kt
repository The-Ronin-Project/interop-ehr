package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Represents an Epic SubLine from AppOrchard.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195#1ParamType18987)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SubLine(
    val subLineNumber: Int,
    val value: String
)
