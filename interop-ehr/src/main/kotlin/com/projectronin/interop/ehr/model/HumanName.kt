package com.projectronin.interop.ehr.model

import com.projectronin.interop.fhir.r4.valueset.NameUse

/**
 * Representation of a human's name.
 */
interface HumanName : EHRElement {
    /**
     * The use of a human name.
     */
    val use: NameUse?

    /**
     * Family name (often called 'Surname')
     */
    val family: String?

    /**
     * Given names (not always 'first'). Includes middle names. Given names appear in the correct order for presenting the name.
     */
    val given: List<String>
}
