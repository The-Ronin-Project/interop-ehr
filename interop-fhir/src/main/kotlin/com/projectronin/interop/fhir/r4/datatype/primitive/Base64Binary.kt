package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * A stream of bytes, base64 encoded (RFC 4648)
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#base64Binary)
 */
data class Base64Binary @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex = Regex("""(\s*([0-9a-zA-Z\+\=]){4}\s*)+""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for a Base64Binary" }
    }
}
