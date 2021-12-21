package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](http://hl7.org/fhir/R4/valueset-identifier-use.html)
 */
enum class IdentifierUse(@JsonValue override val code: String) : CodedEnum<IdentifierUse> {
    /**
     * The identifier recommended for display and use in real-world interactions.
     */
    USUAL("usual"),

    /**
     * The identifier considered to be most trusted for the identification of this item. Sometimes also known as
     * "primary" and "main". The determination of "official" is subjective and implementation guides often provide
     * additional guidelines for use.
     */
    OFFICIAL("official"),

    /**
     * A temporary identifier.
     */
    TEMPORARY("temp"),

    /**
     * An identifier that was assigned in secondary use - it serves to identify the object in a relative context, but
     * cannot be consistently assigned to the same object again in a different context.
     */
    SECONDARY("secondary"),

    /**
     * The identifier id no longer considered valid, but may be relevant for search purposes. E.g. Changes to identifier schemes, account merges, etc.
     */
    OLD("old")
}
