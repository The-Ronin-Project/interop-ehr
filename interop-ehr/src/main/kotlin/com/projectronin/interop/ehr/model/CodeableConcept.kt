package com.projectronin.interop.ehr.model

/**
 * Concept - reference to a terminology or just text
 */
interface CodeableConcept : EHRElement {
    /**
     * Code defined by a terminology system.
     */
    val coding: List<Coding>

    /**
     * Plain text representation of the concept.
     */
    val text: String?
}
