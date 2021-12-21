package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime

/**
 * A time period defined by a start and end date/time.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#period)
 */
data class Period(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val start: DateTime? = null,
    val end: DateTime? = null
) : Element
