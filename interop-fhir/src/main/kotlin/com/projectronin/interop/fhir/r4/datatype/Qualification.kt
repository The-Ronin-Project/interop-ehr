package com.projectronin.interop.fhir.r4.datatype

/**
 * The official certifications, training, and licenses that authorize or otherwise pertain to the provision of care by
 * the practitioner. For example, a medical license issued by a medical board authorizing the practitioner to practice
 * medicine within a certian locality.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/practitioner-definitions.html#Practitioner.qualification)
 */
data class Qualification(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val identifier: List<Identifier> = listOf(),
    val code: CodeableConcept,
    val period: Period? = null,
    val issuer: Reference? = null
) : BackboneElement
