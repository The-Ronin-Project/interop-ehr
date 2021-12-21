package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * A comparator-less Quantity.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/datatypes.html#SimpleQuantity)
 */
data class SimpleQuantity(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val value: Double? = null,
    override val unit: String? = null,
    override val system: Uri? = null,
    override val code: Code? = null
) : BaseQuantity(id, extension, value, null, unit, system, code)
