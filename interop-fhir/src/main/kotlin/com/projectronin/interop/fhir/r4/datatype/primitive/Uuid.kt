package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * A UUID (aka GUID) represented as a URI (RFC 4122); e.g. urn:uuid:c757873d-ec9a-4326-a141-556f43239520
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#uuid)
 */
data class Uuid @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex = Regex("""urn:uuid:[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for an Uuid" }
    }
}
