package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant

/**
 * A Signature holds an electronic representation of a signature and its supporting context in a FHIR accessible form.
 * The signature may either be a cryptographic type (XML DigSig or a JWS), which is able to provide non-repudiation proof,
 * or it may be a graphical image that represents a signature or a signature process.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#Signature)
 */
data class Signature(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val type: List<Coding>,
    val `when`: Instant,
    val who: Reference,
    val onBehalfOf: Reference? = null,
    val targetFormat: Code? = null,
    val sigFormat: Code? = null,
    val data: Base64Binary? = null
) : Element {
    init {
        require(type.isNotEmpty()) { "At least one type is required" }
    }
}
