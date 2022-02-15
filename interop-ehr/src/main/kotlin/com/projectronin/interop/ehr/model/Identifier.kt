package com.projectronin.interop.ehr.model

/**
 * Representation of an Identifier.
 */
interface Identifier : EHRElement {
    /**
     * The namespace for the identifier [value].
     */
    val system: String?

    /**
     * Type of [value]: NPI, driver's license, SSN, "Internal" to the vendor, "External" to the vendor, etc.
     */
    val type: CodeableConcept?

    /**
     * The value that is unique.
     */
    val value: String
}
