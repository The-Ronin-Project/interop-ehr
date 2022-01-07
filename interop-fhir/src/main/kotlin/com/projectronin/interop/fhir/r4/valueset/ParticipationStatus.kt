package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](https://www.hl7.org/fhir/valueset-participationstatus.html)
 */
enum class ParticipationStatus(@JsonValue override val code: String) : CodedEnum<AppointmentStatus> {
    ACCEPTED("accepted"),
    DECLINED("declined"),
    TENTATIVE("tentative"),
    NEEDS_ACTION("needs-action")
}
