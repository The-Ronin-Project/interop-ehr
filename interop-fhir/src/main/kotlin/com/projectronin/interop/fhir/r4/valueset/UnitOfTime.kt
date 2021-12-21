package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](http://hl7.org/fhir/R4/valueset-units-of-time.html)
 */
enum class UnitOfTime(@JsonValue override val code: String) : CodedEnum<UnitOfTime> {
    SECOND("s"),
    MINUTE("min"),
    HOUR("h"),
    DAY("d"),
    WEEK("wk"),
    MONTH("mo"),
    YEAR("a")
}
