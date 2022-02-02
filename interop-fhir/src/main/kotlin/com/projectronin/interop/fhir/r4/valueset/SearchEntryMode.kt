package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * Why an entry is in the result set - whether it's included as a match or because of an _include requirement, or to convey information or warning information about the search process.
 *
 * See [FHIR Spec](http://www.hl7.org/fhir/valueset-search-entry-mode.html)
 */
enum class SearchEntryMode(@JsonValue override val code: String) : CodedEnum<SearchEntryMode> {
    /**
     * This resource matched the search specification.
     */
    MATCH("match"),

    /**
     * This resource is returned because it is referred to from another resource in the search set.
     */
    INCLUDE("include"),

    /**
     * An OperationOutcome that provides additional information about the processing of a search.
     */
    OUTCOME("outcome")
}
