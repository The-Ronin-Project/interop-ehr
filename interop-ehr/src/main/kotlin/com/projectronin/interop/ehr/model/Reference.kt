package com.projectronin.interop.ehr.model

import com.projectronin.interop.fhir.r4.datatype.Identifier

/**
 * Representation of a [Reference].
 */
interface Reference : EHRElement {

    /**
     * The direct reference to the object
     */
    val id: String?

    /**
     * Literal reference
     */
    val reference: String?

    /**
     * Text alternative for reference.
     */
    val display: String?

    /**
     * Identifier of the reference
     */
    val identifier: Identifier?

    /**
     * Type of participant
     */
    val type: String?
}

object ReferenceTypes {
    const val PRACTITIONER = "Practitioner"
    const val PATIENT = "Patient"
}
