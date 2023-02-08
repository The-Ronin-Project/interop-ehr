package com.projectronin.interop.ehr.inputs

/**
 * A message with specified [text] regarding a patient by [patientFHIRID] to be sent to an EHR with one or more [recipients].
 */
data class EHRMessageInput(
    val text: String,
    val patientFHIRID: String,
    val recipients: List<EHRRecipient>,
)
