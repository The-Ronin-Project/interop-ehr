package com.projectronin.interop.ehr.model

/**
 * Representation of a Period.
 */
interface Period : EHRElement {
    /**
     * Starting time with inclusive boundary
     */
    val start: String?

    /**
     * End time with inclusive boundary, if not ongoing
     */
    val end: String?
}
