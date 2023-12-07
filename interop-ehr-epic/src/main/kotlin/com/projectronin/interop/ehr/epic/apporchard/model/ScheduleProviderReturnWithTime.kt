package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Represents an Epic [ScheduleProviderReturnWithTime] from AppOrchard.
 *
 * See [GetPatientAppointments](https://apporchard.epic.com/Sandbox?api=195#1ParamType18970)
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class ScheduleProviderReturnWithTime(
    val departmentIDs: List<IDType> = listOf(),
    val departmentName: String,
    val duration: String,
    val providerIDs: List<IDType> = listOf(),
    val providerName: String,
    val time: String,
)
