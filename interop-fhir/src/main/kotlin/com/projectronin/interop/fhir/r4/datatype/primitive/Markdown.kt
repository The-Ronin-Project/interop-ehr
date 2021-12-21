package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * A FHIR string that may contain markdown syntax for optional processing by a markdown presentation engine, in the GFM extension of CommonMark format
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#markdown)
 */
data class Markdown @JsonCreator constructor(override val value: String) : Primitive<String>
