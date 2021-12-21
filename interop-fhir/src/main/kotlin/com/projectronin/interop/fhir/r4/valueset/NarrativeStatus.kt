package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

enum class NarrativeStatus(@JsonValue override val code: String) : CodedEnum<NarrativeStatus> {
    /**
     * The contents of the narrative are entirely generated from the core elements in the content.
     */
    GENERATED("generated"),

    /**
     * 	The contents of the narrative are entirely generated from the core elements in the content and some of the content
     * 	is generated from extensions. The narrative SHALL reflect the impact of all modifier extensions.
     */
    EXTENSIONS("extensions"),

    /**
     * The contents of the narrative may contain additional information not found in the structured data. Note that there
     * is no computable way to determine what the extra information is, other than by human inspection.
     */
    ADDITIONAL("additional"),

    /**
     * The contents of the narrative are some equivalent of "No human-readable text provided in this case".
     */
    EMPTY("empty")
}
