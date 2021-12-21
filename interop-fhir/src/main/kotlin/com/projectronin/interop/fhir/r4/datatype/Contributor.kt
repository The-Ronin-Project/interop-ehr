package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.valueset.ContributorType

/**
 * See [FHIR Documentation](http://hl7.org/fhir/R4/metadatatypes.html#Contributor)
 */
data class Contributor(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val type: ContributorType,
    val name: String,
    val contact: List<ContactDetail> = listOf()
) : Element
