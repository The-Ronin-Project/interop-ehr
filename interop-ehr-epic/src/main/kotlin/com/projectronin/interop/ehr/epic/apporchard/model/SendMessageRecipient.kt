package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data class for making requests to Epic's [SendMessage Recipient](https://apporchard.epic.com/Sandbox?api=384#1ParamType374787) object.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SendMessageRecipient(
    @JsonProperty("ID")
    val iD: String,
    @JsonProperty("IsPool")
    val isPool: Boolean,
    @JsonProperty("IDType")
    val iDType: String = "External"
)
