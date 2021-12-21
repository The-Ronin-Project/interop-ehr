package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.QuantityComparator

/**
 * A measured amount (or an amount that can potentially be measured).
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/datatypes.html#quantity)
 */
data class Quantity(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val value: Double? = null,
    override val comparator: QuantityComparator? = null,
    override val unit: String? = null,
    override val system: Uri? = null,
    override val code: Code? = null
) : BaseQuantity(id, extension, value, comparator, unit, system, code)
