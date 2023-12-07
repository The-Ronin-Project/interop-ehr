package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Practitioner
import java.time.LocalDateTime

data class NoteInput(
    val noteText: String,
    val dateTime: LocalDateTime,
    val noteSender: NoteSender,
    val isAlert: Boolean,
    val patient: Patient,
    val practitioner: Practitioner,
)

enum class NoteSender {
    PATIENT,
    PRACTITIONER,
}
