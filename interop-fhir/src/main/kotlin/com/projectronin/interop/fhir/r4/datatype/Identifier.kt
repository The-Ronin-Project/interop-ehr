package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse

/**
 * A numeric or alphanumeric string that is associated with a single object or entity within a given system. Typically,
 * identifiers are used to connect content in resources to external content available in other frameworks or protocols.
 * Identifiers are associated with objects and may be changed or retired due to human or system process and errors.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#Identifier)
 */
data class Identifier(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val use: IdentifierUse? = null,
    val type: CodeableConcept? = null,
    val system: Uri? = null,
    val value: String? = null,
    val period: Period? = null,
    val assigner: Reference? = null
) : Element {
    init {
        assigner?.type?.let {
            require(it.value == "Organization") { "Assigner must belong to an Organization" }
        }
    }
}
