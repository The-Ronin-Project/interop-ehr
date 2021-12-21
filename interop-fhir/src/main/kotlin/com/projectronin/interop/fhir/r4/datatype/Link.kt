package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.valueset.LinkType

/**
 * Link to another patient resource that concerns the same actual patient.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/patient-definitions.html#Patient.link)
 */
data class Link(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val other: Reference,
    val type: LinkType
) : BackboneElement
