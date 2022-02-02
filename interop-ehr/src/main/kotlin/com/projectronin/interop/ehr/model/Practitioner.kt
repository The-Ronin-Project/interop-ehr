package com.projectronin.interop.ehr.model

import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

/**
 * Representation of a Practitioner.
 */
interface Practitioner : EHRResource {
    /**
     * Logical ID of this [Practitioner].
     */
    val id: String

    /**
     * Identifiers for this [Practitioner].
     */
    val identifier: List<Identifier>

    /**
     * Whether the [Practitioner] is active.
     */
    val active: Boolean?

    /**
     * Languages that the [Practitioner] can use for patient communication.
     */
    val communication: List<CodeableConcept>

    /**
     * [Practitioner]'s gender
     */
    val gender: AdministrativeGender?

    /**
     * [Practitioner]'s name
     */
    val name: List<HumanName>

    /**
     * [Practitioner]'s contact info
     */
    val telecom: List<ContactPoint>
}
