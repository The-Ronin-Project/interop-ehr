package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Data class for making requests to Epic's [SendMessage](https://apporchard.epic.com/Sandbox?api=384) api.
 *
 * @param contactID: The contact identifier for the patient contact.
 * @param contactIDType: The contact type for the ContactID. CSN or UCN.
 * @param messageText: Message text to include with message. Only plain text is supported.
 * @param messageType: The message type of the message being sent. This must correspond to a value outside of Epic's release range or part of the safelist. The safelist currently includes only value 1-Staff Message. This element also accepts the title.
 * @param patientID: The patient identifier to include with the message.
 * @param patientIDType: The type of patient ID being passed in. Possibilities are CID, RecordID (can be internal or external), Name, NationalID, or IIT identifier (such as an MRN).
 * @param recipients: The recipients of the message. This is an Array of object type Recipient.
 * @param senderID: An identifier corresponding to a user to be identified as the sender of the message. Type of identifier is given in SenderIDType.
 * @param senderIDType: The ID Type for the SenderID. Posibilities are CID, RecordID (can be internal or external), Name, SystemLogin, or Alias. Default is internal.
 * @param messagePriority: The priority of the message to be sent. If not passed in, the default priority will be Routine. Also accepts the title.
 */
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SendMessageRequest(
    val messageText: String?,
    val patientID: String?,
    val recipients: List<SendMessageRecipient>?,
    val senderID: String?,
    val messageType: String?,
    val senderIDType: String = "External",
    val patientIDType: String = "MRN",
    val contactID: String? = "",
    val contactIDType: String? = "",
    val messagePriority: String? = ""
)
