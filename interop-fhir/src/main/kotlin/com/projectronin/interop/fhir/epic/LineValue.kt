package com.projectronin.interop.fhir.epic

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Represents an Epic LineValue from AppOrchard.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195#2ParamType96)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class LineValue(
    val lineNumber: Int,
    val subLines: List<SubLine> = listOf(),
    val value: String
)
