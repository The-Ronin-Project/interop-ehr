package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * A Coding is a representation of a defined concept using a symbol from a defined "code system"
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/datatypes.html#Coding)
 */
data class Coding(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val system: Uri? = null,
    val version: String? = null,
    val code: Code? = null,
    val display: String? = null,
    val userSelected: Boolean? = null
) : Element
