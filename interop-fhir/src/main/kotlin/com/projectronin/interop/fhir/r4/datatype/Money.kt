package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Code

/**
 * An amount of currency.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#Money)
 */
data class Money(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val value: Double? = null,
    val currency: Code? = null
) : Element
