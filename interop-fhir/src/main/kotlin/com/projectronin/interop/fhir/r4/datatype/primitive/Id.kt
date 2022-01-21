package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Any combination of upper- or lower-case ASCII letters ('A'..'Z', and 'a'..'z', numerals ('0'..'9'), '-' and '.'
 * FHIR recommends a length limit of 64 characters which we are not enforcing.
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#id)
 */
data class Id @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex = Regex("""[A-Za-z0-9\-\.]+""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for an Id" }
    }
}
