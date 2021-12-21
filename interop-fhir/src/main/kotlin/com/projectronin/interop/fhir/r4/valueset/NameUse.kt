package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](https://www.hl7.org/fhir/valueset-name-use.html)
 */
enum class NameUse(@JsonValue override val code: String, val parent: NameUse? = null) : CodedEnum<NameUse> {
    /**
     * Known as/conventional/the one you normally use.
     */
    USUAL("usual"),

    /**
     * The formal name as registered in an official (government) registry, but which name might not be commonly used. May be called "legal name".
     */
    OFFICIAL("official"),

    /**
     * A temporary name. Name.period can provide more detailed information. This may also be used for temporary names assigned at birth or in emergency situations.
     */
    TEMPORARY("temp"),

    /**
     * A name that is used to address the person in an informal manner, but is not part of their formal or usual name.
     */
    NICKNAME("nickname"),

    /**
     * Anonymous assigned name, alias, or pseudonym (used to protect a person's identity for privacy reasons).
     */
    ANONYMOUS("anonymous"),

    /**
     * This name is no longer in use (or was never correct, but retained for records).
     */
    OLD("old"),

    /**
     * A name used prior to changing name because of marriage. This name use is for use by applications that collect and store names that were used prior to a marriage. Marriage naming customs vary greatly around the world, and are constantly changing. This term is not gender specific. The use of this term does not imply any particular history for a person's name.
     */
    MAIDEN("maiden", OLD)
}
