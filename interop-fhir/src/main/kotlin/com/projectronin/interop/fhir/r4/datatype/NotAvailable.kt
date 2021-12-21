package com.projectronin.interop.fhir.r4.datatype

/**
 * The practitioner is not available or performing this role during this period of time due to the provided reason.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/practitionerrole-definitions.html#PractitionerRole.notAvailable)
 */
data class NotAvailable(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val description: String,
    val during: Period? = null
) : BackboneElement
