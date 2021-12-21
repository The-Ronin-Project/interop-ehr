package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](http://hl7.org/fhir/R4/valueset-sort-direction.html)
 */
enum class SortDirection(@JsonValue override val code: String) : CodedEnum<SortDirection> {
    ASCENDING("ascending"),
    DESCENDING("descending")
}
