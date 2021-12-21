package com.projectronin.interop.ehr.model

import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse

/**
 * Representation of a contact point.
 */
interface ContactPoint : EHRElement {
    /**
     * Telecommunications form for contact point
     */
    val system: ContactPointSystem?

    /**
     * Use of contact point
     */
    val use: ContactPointUse?

    /**
     * The actual contact point details
     */
    val value: String?
}
