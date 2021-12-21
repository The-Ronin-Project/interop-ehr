package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](https://www.hl7.org/fhir/valueset-administrative-gender.html)
 */
enum class AdministrativeGender(@JsonValue override val code: String) : CodedEnum<AdministrativeGender> {
    MALE("male"),
    FEMALE("female"),
    OTHER("other"),
    UNKNOWN("unknown")
}
