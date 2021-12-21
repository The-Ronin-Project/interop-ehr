package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](https://hl7.org/fhir/R4/valueset-quantity-comparator.html)
 */
enum class QuantityComparator(@JsonValue override val code: String) : CodedEnum<QuantityComparator> {
    /**
     * The actual value is less than the given value.
     */
    LESS_THAN("<"),

    /**
     * The actual value is less than or equal to the given value.
     */
    LESS_OR_EQUAL_TO("<="),

    /**
     * The actual value is greater than or equal to the given value.
     */
    GREATER_OR_EQUAL_TO(">="),

    /**
     * The actual value is greater than the given value.
     */
    GREATER_THAN(">")
}
