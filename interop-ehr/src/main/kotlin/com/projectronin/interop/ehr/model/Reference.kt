package com.projectronin.interop.ehr.model
/**
 * Representation of a [Reference].
 */
interface Reference : EHRElement, EHRElementID {
    enum class ReferenceType {
        Provider
    }
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
    val type: ReferenceType?
}
