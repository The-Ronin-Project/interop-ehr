package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](http://hl7.org/fhir/valueset-appointmentstatus.html)
 */
enum class AppointmentStatus(@JsonValue override val code: String) : CodedEnum<AppointmentStatus> {
    PROPOSED("proposed"),
    PENDING("pending"),
    BOOKED("booked"),
    ARRIVED("arrived"),
    FULFILLED("fulfilled"),
    CANCELLED("cancelled"),
    NOSHOW("noshow"),
    ENTERED_IN_ERROR("entered-in-error"),
    CHECKED_IN("checked-in"),
    WAITLIST("waitlist")
}
