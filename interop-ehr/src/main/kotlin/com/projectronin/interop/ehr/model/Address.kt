package com.projectronin.interop.ehr.model

import com.projectronin.interop.fhir.r4.valueset.AddressUse

/**
 * Representation of an Address.
 */
interface Address : EHRElement {
    /**
     * The use of an address.
     */
    val use: AddressUse?

    /**
     * Street name, number, direction & P.O. Box etc. The order in which lines should appear in an address label
     */
    val line: List<String>

    /**
     * Name of city, town etc.
     */
    val city: String?

    /**
     * Sub-unit of country (abbreviations ok)
     */
    val state: String?

    /**
     * Postal code for area
     */
    val postalCode: String?
}
