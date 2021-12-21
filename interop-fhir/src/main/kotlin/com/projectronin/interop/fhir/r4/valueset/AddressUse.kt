package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](https://www.hl7.org/fhir/valueset-address-use.html)
 */
enum class AddressUse(@JsonValue override val code: String) : CodedEnum<AddressUse> {
    /**
     * A communication address at a home.
     */
    HOME("home"),

    /**
     * An office address. First choice for business related contacts during business hours.
     */
    WORK("work"),

    /**
     * A temporary address. The period can provide more detailed information.
     */
    TEMPORARY("temp"),

    /**
     * This address is no longer in use (or was never correct but retained for records).
     */
    OLD("old"),

    /**
     * An address to be used to send bills, invoices, receipts etc.
     */
    BILLING("billing")
}
