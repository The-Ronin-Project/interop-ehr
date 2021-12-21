package com.projectronin.interop.ehr.model

/**
 * Base interface defining EHR elements.
 */
interface EHRElement {
    /**
     * The raw data from an EHR backing this model.
     */
    val raw: String
}
