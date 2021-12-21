package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](http://hl7.org/fhir/R4/valueset-operation-parameter-use.html)
 */
enum class OperationParameterUse(@JsonValue override val code: String) : CodedEnum<OperationParameterUse> {
    INPUT("in"),
    OUTPUT("out")
}
