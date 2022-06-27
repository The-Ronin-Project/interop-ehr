package com.projectronin.interop.ehr.model

/**
 * Representation of a Reference Range.
 */
interface ReferenceRange : EHRElement {

    val high: SimpleQuantity?

    val low: SimpleQuantity?

    val text: String?
}
