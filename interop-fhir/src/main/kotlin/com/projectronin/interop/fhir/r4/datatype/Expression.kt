package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * The Expression structure defines an expression that generates a value. The expression is provided in a specified
 * language (by mime type). The context of use of the expression must specify the context in which the expression is
 * evaluated, and how the result of the expression is used.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/metadatatypes.html#Expression)
 */
data class Expression(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val description: String? = null,
    val name: Id? = null,
    val language: Code,
    val expression: String? = null,
    val reference: Uri? = null
) : Element {
    init {
        require(expression != null || reference != null) { "An expression or a reference must be provided" }
    }
}
