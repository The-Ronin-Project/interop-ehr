package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.swagger.v3.oas.annotations.media.Schema

@Schema
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SetSmartDataValuesResult(
    val errors: List<SmartDataError> = listOf(),
    val success: Boolean = false,
)

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SmartDataError(
    val code: String,
    val smartDataID: String,
    val smartDataIDType: String,
    val message: String,
)
