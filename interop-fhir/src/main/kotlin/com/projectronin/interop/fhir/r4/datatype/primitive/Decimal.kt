package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * 	Rational numbers that have a decimal representation.
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#decimal)
 */
data class Decimal @JsonCreator constructor(override val value: Double) : Primitive<Double>
