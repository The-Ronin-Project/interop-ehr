package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * A date, or partial date (e.g. just year or year + month) as used in human communication. The format is YYYY, YYYY-MM,
 * or YYYY-MM-DD, e.g. 2018, 1973-06, or 1905-08-23. There SHALL be no time zone. Dates SHALL be valid dates
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#date)
 */
data class Date @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex =
        Regex("""([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1]))?)?""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for a Date" }
    }
}
