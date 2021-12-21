package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * An OID represented as a URI (RFC 3001); e.g. urn:oid:1.2.3.4.5
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#oid)
 */
data class Oid @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex = Regex("""urn:oid:[0-2](\.(0|[1-9][0-9]*))+""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for an Oid" }
    }
}
