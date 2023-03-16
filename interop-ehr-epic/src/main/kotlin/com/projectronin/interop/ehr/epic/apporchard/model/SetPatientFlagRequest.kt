package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.swagger.v3.oas.annotations.media.Schema

@Schema
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SetPatientFlagRequest(val flag: PatientFlag)

@Schema
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class PatientFlag(
    @JsonProperty("ID")
    val id: String? = null, // used if we want to set a specific flag
    @JsonProperty("IDType")
    val idType: String? = null,
    val status: String? = "1", // 1 = on, null = off
    val summary: String? = null,
    val text: List<String>? = null,
    val type: String?
)
