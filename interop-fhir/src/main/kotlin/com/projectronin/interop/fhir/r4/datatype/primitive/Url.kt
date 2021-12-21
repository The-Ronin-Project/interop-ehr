package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * 	A Uniform Resource Locator (RFC 1738 ). Note URLs are accessed directly using the specified protocol.
 * 	Common URL protocols are http{s}:, ftp:, mailto: and mllp:, though many others are defined
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#url)
 */
data class Url @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex = Regex("""\S*""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for an Url" }
    }
}
