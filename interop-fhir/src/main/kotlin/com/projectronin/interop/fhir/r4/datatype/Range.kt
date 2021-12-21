package com.projectronin.interop.fhir.r4.datatype

/**
 * A set of ordered Quantity values defined by a low and high limit. A Range specifies a set of possible values;
 * usually, one value from the range applies (e.g. "give the patient between 2 and 4 tablets"). Ranges are typically used in instructions.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#range)
 */
data class Range(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val low: SimpleQuantity? = null,
    val high: SimpleQuantity? = null
) : Element
