package com.projectronin.interop.ehr.model.enums

import com.projectronin.interop.common.enums.CodedEnum

/**
 * The comparator for a quantity defining how to understand the quantity's value.
 */
enum class QuantityComparator(override val code: String) : CodedEnum<String> {
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN_OR_EQUAL(">="),
    GREATER_THAN(">")
}
