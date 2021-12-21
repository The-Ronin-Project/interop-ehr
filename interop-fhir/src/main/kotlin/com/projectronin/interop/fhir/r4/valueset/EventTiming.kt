package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](http://hl7.org/fhir/R4/valueset-event-timing.html)
 */
enum class EventTiming(@JsonValue override val code: String) : CodedEnum<EventTiming> {
    MORNING("MORN"),
    EARLY_MORNING("MORN.early"),
    LATE_MORNING("MORN.late"),
    NOON("NOON"),
    AFTERNOON("AFT"),
    EARLY_AFTERNOON("AFT.early"),
    LATE_AFTERNOON("AFT.late"),
    EVENING("EVE"),
    EARLY_EVENING("EVE.early"),
    LATE_EVENING("EVE.late"),
    NIGHT("NIGHT"),
    AFTER_SLEEP("PHS"),
    PRIOR_REGULAR_SLEEP("HS"),
    UPON_WAKING("WAKE"),
    MEAL("C"),
    BREAKFAST("CM"),
    LUNCH("CD"),
    DINNER("CV"),
    BEFORE_MEAL("AC"),
    BEFORE_BREAKFAST("ACM"),
    BEFORE_LUNCH("ACD"),
    BEFORE_DINNER("ACV"),
    AFTER_MEAL("PC"),
    AFTER_BREAKFAST("PCM"),
    AFTER_LUNCH("PCD"),
    AFTER_DINNER("PCV")
}
