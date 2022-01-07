package com.projectronin.interop.fhir.epic

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Represents an Epic ExtensionInformationReturn from AppOrchard.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195#1ParamType18990)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class ExtensionInformationReturn(
    val extensionIds: List<IdType> = listOf(),
    val extensionName: String,
    val lines: List<LineValue> = listOf(),
    val value: String
)
