package com.projectronin.interop.ehr.model

/**
 * Representation of a Simple Quantity.
 */
interface SimpleQuantity : EHRElement {
    /**
     * Numerical value (with implicit precision)
     */
    val value: Double?

    /**
     * Unit representation
     */
    val unit: String?

    /**
     * System that defines coded unit form
     */
    val system: String?

    /**
     * Coded form of the unit
     */
    val code: String?
}
