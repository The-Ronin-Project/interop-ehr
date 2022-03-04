package com.projectronin.interop.ehr.model

/**
 * Representation of a Range.
 */
interface Range : EHRElement {
    /**
     * Low limit
     */
    val low: SimpleQuantity?

    /**
     * High limit
     */
    val high: SimpleQuantity?
}
