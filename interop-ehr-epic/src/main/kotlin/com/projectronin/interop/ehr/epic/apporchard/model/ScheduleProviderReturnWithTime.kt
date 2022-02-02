package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.annotation.JsonIgnore
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
    val time: String
) {
    /*
     * Provider and department ids are pulled from the "Internal" id.
     * See [Data Platform](https://github.com/projectronin/dp-databricks-jobs/blob/513cd599955f5905dd20623b2714de2ab4d9c3c0/jobs/gold/mdaoc/fhir/appointment.py#L151).
     */
    @get:JsonIgnore
    val providerId: String? by lazy {
        providerIDs.firstOrNull { it.type == "Internal" }?.id
    }

    @get:JsonIgnore
    val departmentId: String? by lazy {
        departmentIDs.firstOrNull { it.type == "Internal" }?.id
    }
}
