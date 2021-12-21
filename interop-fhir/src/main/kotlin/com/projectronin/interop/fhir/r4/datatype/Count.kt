package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.QuantityComparator
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Defined variation of [Quantity] for describing Counts.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#Count)
 */
data class Count(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val value: Double? = null,
    override val comparator: QuantityComparator? = null,
    override val unit: String? = null,
    override val system: Uri? = null,
    override val code: Code? = null
) : BaseQuantity(id, extension, value, comparator, unit, system, code) {
    init {
        require((code != null && code.value == "1") || value == null) { "There SHALL be a code with a value of \"1\" if there is a value" }
        require(system == null || system == CodeSystem.UCUM.uri) { "If system is present, it SHALL be UCUM" }
        require(value == null || ceil(value) == floor(value)) { "If present, the value SHALL be a whole number" }
    }
}
