package com.projectronin.interop.fhir.r4.datatype

/**
 * The ContactDetail structure defines general contact details.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/metadatatypes.html#ContactDetail)
 */
data class ContactDetail(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val name: String? = null,
    val telecom: List<ContactPoint> = listOf()
) : Element
