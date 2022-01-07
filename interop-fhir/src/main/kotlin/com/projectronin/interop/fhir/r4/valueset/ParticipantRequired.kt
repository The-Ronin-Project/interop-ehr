package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](https://www.hl7.org/fhir/valueset-participantrequired.html)
 */
enum class ParticipantRequired(@JsonValue override val code: String) : CodedEnum<AppointmentStatus> {
    REQUIRED("required"),
    OPTIONAL("optional"),
    INFORMATION_ONLY("information-only")
}
