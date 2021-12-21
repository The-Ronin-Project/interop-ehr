package com.projectronin.interop.fhir.r4.datatype

/**
 * The base definition for complex elements defined as part of a resource definition - that is, elements that have children that are defined in the resource.
 *
 * See [FHIR Spec](https://hl7.org/fhir/R4/backboneelement.html)
 */
interface BackboneElement : Element {
    val modifierExtension: List<Extension>
}
