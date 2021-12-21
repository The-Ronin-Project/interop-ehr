package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](https://www.hl7.org/fhir/valueset-link-type.html)
 */
enum class LinkType(@JsonValue override val code: String) : CodedEnum<LinkType> {
    REPLACED_BY("replaced-by"),
    REPLACES("replaces"),
    REFER("refer"),
    SEE_ALSO("see also")
}
