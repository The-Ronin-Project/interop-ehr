package com.projectronin.interop.ehr.model

/**
 * Representation of a Practitioner Role.
 */
interface PractitionerRole : EHRResource {
    /**
     * Logical ID of this [PractitionerRole].
     */
    val id: String

    /**
     * Whether the [PractitionerRole] is active.
     */
    val active: Boolean?

    /**
     * Reference to a practitioner associated with this [PractitionerRole].
     */
    val practitioner: Reference?

    /**
     * List of roles the [Practitioner] may perform.
     */
    val code: List<CodeableConcept>

    /**
     * References to locations where the practitioner provides care.
     */
    val location: List<Reference>

    /**
     * [Practitioner] specialties.
     */
    val specialty: List<CodeableConcept>

    /**
     * [Practitioner] contact information.
     */
    val telecom: List<ContactPoint>
}
