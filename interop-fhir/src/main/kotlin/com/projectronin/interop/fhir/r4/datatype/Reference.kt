package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * A Reference to another resource.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/references.html#Reference)
 */
data class Reference(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val reference: String? = null,
    val type: Uri? = null,
    val identifier: Identifier? = null,
    val display: String? = null
) : Element
