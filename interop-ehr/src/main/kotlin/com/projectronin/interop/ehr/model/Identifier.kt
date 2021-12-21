package com.projectronin.interop.ehr.model

/**
 * Representation of an Identifier.
 */
interface Identifier : EHRElement {
    /**
     * The namespace for the identifier [value].
     */
    val system: String

    /**
     * The value that is unique.
     */
    val value: String
}
