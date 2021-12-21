package com.projectronin.interop.ehr.model

import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

/**
 * Representation of a Patient.
 */
interface Patient : EHRResource {
    /**
     * Logical ID of this patient.
     */
    val id: String

    /**
     * Identifiers for this patient.
     */
    val identifier: List<Identifier>

    /**
     * A name associated with the patient.
     */
    val name: List<HumanName>

    /**
     * The date of birth for the individual.
     */
    val birthDate: String?

    /**
     * The gender of a person used for administrative purposes.
     */
    val gender: AdministrativeGender?

    /**
     * A contact detail for the individual.
     */
    val telecom: List<ContactPoint>

    /**
     * An address for the individual.
     */
    val address: List<Address>
}
