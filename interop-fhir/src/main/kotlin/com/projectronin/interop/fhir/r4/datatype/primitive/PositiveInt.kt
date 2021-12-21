package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Any positive integer in the range 1..2,147,483,647
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#positiveInt)
 */
data class PositiveInt @JsonCreator constructor(override val value: Int) : Primitive<Int> {
    init {
        require(value > 0) { "Supplied value is not valid for a PositiveInt" }
    }
}
