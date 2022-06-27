package com.projectronin.interop.ehr.model

/**
 * Representation of a Ratio.
 */
interface Ratio : EHRElement {

    val numerator: Quantity?

    val denominator: Quantity?
}
