package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Indicates that the value is taken from a set of controlled strings defined elsewhere. Technically, a code is restricted to a string
 * which has at least one character and no leading or trailing whitespace, and where there is no whitespace other than single spaces in the contents
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#code)
 */
data class Code @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex = Regex("""[^\s]+(\s[^\s]+)*""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for a Code" }
    }
}
