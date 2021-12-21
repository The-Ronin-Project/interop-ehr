package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.QuantityComparator

abstract class BaseQuantity(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    open val value: Double? = null,
    open val comparator: QuantityComparator? = null,
    open val unit: String? = null,
    open val system: Uri? = null,
    open val code: Code? = null
) : Element {
    init {
        require(code == null || system != null) { "If a code for the unit is present, the system SHALL also be present" }
    }
}
