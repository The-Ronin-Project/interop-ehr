package com.projectronin.interop.ehr.model

/**
 * Representation of a [Condition].
 */
interface Condition : EHRResource {
    /**
     * Logical ID of this [Condition]
     */
    val id: String

    /**
     * External identifiers for this [Condition]
     */
    val identifier: List<Identifier>

    /**
     * Clinical status of this [Condition] (e.g. active, relapse, recurrence, etc)
     */
    val clinicalStatus: CodeableConcept?

    /**
     * Category of this [Condition] (e.g. problem-list-item, encounter-diagnosis, etc)
     */
    val category: List<CodeableConcept>

    /**
     * Identification of the [Condition], problem or diagnosis
     */
    val code: CodeableConcept?
}
