package com.projectronin.interop.ehr.inputs

/**
 * A message with specified [text] regarding a patient by [patientMRN] to be sent to an EHR with one or more [recipients].
 */
data class EHRMessageInput(
    val text: String,
    val patientMRN: String,
    val recipients: List<EHRRecipient>
)
