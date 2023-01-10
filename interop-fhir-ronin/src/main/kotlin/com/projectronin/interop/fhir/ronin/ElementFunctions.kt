package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.element.Element

/**
 * True when any [Element] in the [List] contains a data absent reason in its extensions.
 */
fun <T : Element<T>> List<T>?.hasDataAbsentReason(): Boolean {
    return this?.any { it.hasDataAbsentReason() }
        ?: false
}

/**
 * True when an [Element] contains a data absent reason in its extensions.
 */
fun <T : Element<T>> T?.hasDataAbsentReason(): Boolean {
    return this?.extension?.any { it.url?.value == "http://hl7.org/fhir/StructureDefinition/data-absent-reason" }
        ?: false
}
