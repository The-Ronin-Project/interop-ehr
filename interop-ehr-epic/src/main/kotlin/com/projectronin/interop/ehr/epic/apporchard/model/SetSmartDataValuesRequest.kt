package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.swagger.v3.oas.annotations.media.Schema

@Schema
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SetSmartDataValuesRequest(
    @JsonProperty("EntityID")
    val id: String,
    @JsonProperty("EntityIDType")
    val idType: String? = null,
    val userID: String = "1",
    val userIDType: String = "External",
    val source: String = "Ronin",
    val contextName: String = "PATIENT",
    val smartDataValues: List<SmartDataValue>
)

@Schema
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SmartDataValue(
    val comments: List<String> = listOf(),
    val smartDataID: String,
    val smartDataIDType: String,
    val values: String
)
