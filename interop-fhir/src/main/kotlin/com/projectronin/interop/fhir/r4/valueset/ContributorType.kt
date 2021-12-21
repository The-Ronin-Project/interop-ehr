package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](http://hl7.org/fhir/R4/valueset-contributor-type.html)
 */
enum class ContributorType(@JsonValue override val code: String) : CodedEnum<ContributorType> {
    /**
     * An author of the content of the module.
     */
    AUTHOR("author"),

    /**
     * An editor of the content of the module.
     */
    EDITOR("editor"),

    /**
     * A reviewer of the content of the module.
     */
    REVIEWER("reviewer"),

    /**
     * An endorser of the content of the module.
     */
    ENDORSER("endorser")
}
