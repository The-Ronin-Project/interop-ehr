package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * A URI that refers to a resource by its canonical URL (resources with a url property). The canonical type differs from a
 * uri in that it has special meaning in this specification, and in that it may have a version appended, separated by a
 * vertical bar (|). Note that the type canonical is not used for the actual canonical URLs that are the target of these
 * references, but for the URIs that refer to them, and may have the version suffix in them. Like other URIs, elements
 * of type canonical may also have #fragment references
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#canonical)
 */
data class Canonical @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex = Regex("""\S*""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for a Canonical" }
    }
}
