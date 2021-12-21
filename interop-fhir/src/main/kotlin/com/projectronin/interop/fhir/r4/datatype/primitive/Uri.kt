package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * A Uniform Resource Identifier Reference (RFC 3986). URIs can be absolute or relative, and may have an optional fragment identifier
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#uri)
 */
data class Uri @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex = Regex("""\S*""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for an Uri" }
    }
}
