package com.projectronin.interop.fhir.r4.datatype

/**
 * The base definition for all elements contained inside a resource.
 *
 * See [FHIR Spec](https://hl7.org/fhir/R4/element.html)
 */
interface Element {
    val id: String?
    val extension: List<Extension>
}
