package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Represents an Epic ExtensionInformationReturn from AppOrchard.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195#1ParamType18990)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class ExtensionInformationReturn(
    val extensionIds: List<IDType> = listOf(),
    val extensionName: String,
    val lines: List<LineValue> = listOf(),
    val value: String
)
