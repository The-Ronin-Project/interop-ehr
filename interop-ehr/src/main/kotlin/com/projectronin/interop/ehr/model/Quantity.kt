package com.projectronin.interop.ehr.model

import com.projectronin.interop.ehr.model.enums.QuantityComparator

/**
 * Representation of a Quantity.
 */
interface Quantity : SimpleQuantity {
    /**
     * How to understand the value
     */
    val comparator: QuantityComparator?
}
