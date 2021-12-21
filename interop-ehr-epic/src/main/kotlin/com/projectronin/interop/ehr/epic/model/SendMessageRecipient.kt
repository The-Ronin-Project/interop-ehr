package com.projectronin.interop.ehr.epic.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Data class for making requests to Epic's [SendMessage Recipient](https://apporchard.epic.com/Sandbox?api=384#1ParamType374787) object.
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SendMessageRecipient(
    val iD: String,
    val isPool: Boolean,
    val iDType: String = "External"
)
