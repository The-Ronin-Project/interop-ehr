package com.projectronin.interop.ehr.model

/**
 * Base interface defining EHR elements.
 */
interface EHRElement {
    /**
     * The raw data from an EHR backing this model.
     */
    val raw: String

    /**
     * The element from an EHR backing this model. The exact type should be determined based off the data source for the
     * resource from which this element was extracted.
     */
    val element: Any
}
