package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](https://www.hl7.org/fhir/valueset-address-type.html)
 */
enum class AddressType(@JsonValue override val code: String) : CodedEnum<AddressType> {
    /**
     * Mailing addresses - PO Boxes and care-of addresses.
     */
    POSTAL("postal"),

    /**
     * A physical address that can be visited.
     */
    PHYSICAL("physical"),

    /**
     * An address that is both physical and postal.
     */
    BOTH("both")
}
