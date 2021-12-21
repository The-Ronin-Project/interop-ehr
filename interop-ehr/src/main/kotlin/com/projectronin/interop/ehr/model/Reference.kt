package com.projectronin.interop.ehr.model

/**
 * Representation of a [Reference].
 */
interface Reference : EHRElement {
    /**
     * Literal reference
     */
    val reference: String

    /**
     * Text alternative for reference.
     */
    val display: String?
}
