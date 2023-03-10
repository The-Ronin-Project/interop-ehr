package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.swagger.v3.oas.annotations.media.Schema
@Schema
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SetPatientFlagResponse(
    val error: String = "",
    val success: Boolean = false
)
