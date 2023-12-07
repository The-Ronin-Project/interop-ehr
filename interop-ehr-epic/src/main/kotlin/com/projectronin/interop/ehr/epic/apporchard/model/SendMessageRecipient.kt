package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Data class for making requests to Epic's [SendMessage Recipient](https://apporchard.epic.com/Sandbox?api=384#1ParamType374787) object.
 */

@Schema
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SendMessageRecipient(
    @JsonProperty("ID")
    @field:Schema(example = "IPMD", type = "string")
    val iD: String,
    @JsonProperty("IsPool")
    val isPool: Boolean,
    @JsonProperty("IDType")
    val iDType: String = "External",
)
