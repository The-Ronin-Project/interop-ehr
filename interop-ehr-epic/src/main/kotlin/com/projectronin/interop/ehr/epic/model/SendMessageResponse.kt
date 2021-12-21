package com.projectronin.interop.ehr.epic.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Data class for receiving responses from Epic's [SendMessage](https://apporchard.epic.com/Sandbox?api=384) api.
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SendMessageResponse(@JsonProperty("IDTypes") val iDTypes: List<IDType>)
