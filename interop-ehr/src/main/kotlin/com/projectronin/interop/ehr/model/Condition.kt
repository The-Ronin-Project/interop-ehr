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
     * Verification status of this [Condition]
     */
    val verificationStatus: CodeableConcept?

    /**
     * Category of this [Condition] (e.g. problem-list-item, encounter-diagnosis, etc)
     */
    val category: List<CodeableConcept>

    /**
     * Subjective severity of condition
     */
    val severity: CodeableConcept?

    /**
     * Identification of the [Condition], problem or diagnosis
     */
    val code: CodeableConcept?

    /**
     * Anatomical location, if relevant
     */
    val bodySite: List<CodeableConcept>

    /**
     * Who has the condition?
     */
    val subject: Reference

    /**
     * Encounter created as part of
     */
    val encounter: Reference?

    /**
     * Estimated or actual date, date-time, or age
     */
    val onset: Onset<out Any>?

    /**
     * When in resolution/remission
     */
    val abatement: Abatement<out Any>?

    /**
     * Date record was first recorded
     */
    val recordedDate: String?

    /**
     * Who recorded the condition
     */
    val recorder: Reference?

    /**
     * Person who asserts this condition
     */
    val asserter: Reference?

    /**
     * Stage/grade, usually assessed formally
     */
    val stage: List<Stage>

    /**
     * Supporting evidence
     */
    val evidence: List<Evidence>

    /**
     * Additional information about the Condition
     */
    val note: List<Annotation>

    /**
     * Representation of a condition's onset.
     */
    sealed interface Onset<T> {
        val value: T
    }

    /**
     * Implementation of an Onset based off a date-time [String]
     */
    class DateTimeOnset(override val value: String) : Onset<String>

    /**
     * Implementation of an Onset based off an [Age]
     */
    class AgeOnset(override val value: Age) : Onset<Age>

    /**
     * Implementation of an Onset based off a [Period]
     */
    class PeriodOnset(override val value: Period) : Onset<Period>

    /**
     * Implementation of an Onset based off a [Range]
     */
    class RangeOnset(override val value: Range) : Onset<Range>

    /**
     * Implementation of an Onset based off a [String]
     */
    class StringOnset(override val value: String) : Onset<String>

    /**
     * Representation of a condition's abatement.
     */
    sealed interface Abatement<T> {
        val value: T
    }

    /**
     * Implementation of an Abatement based off a date-time [String]
     */
    class DateTimeAbatement(override val value: String) : Abatement<String>

    /**
     * Implementation of an Abatement based off an [Age]
     */
    class AgeAbatement(override val value: Age) : Abatement<Age>

    /**
     * Implementation of an Abatement based off a [Period]
     */
    class PeriodAbatement(override val value: Period) : Abatement<Period>

    /**
     * Implementation of an Abatement based off a [Range]
     */
    class RangeAbatement(override val value: Range) : Abatement<Range>

    /**
     * Implementation of an Abatement based off a [String]
     */
    class StringAbatement(override val value: String) : Abatement<String>

    /**
     * Representation of a condition's stage.
     */
    interface Stage : EHRElement {
        /**
         * Simple summary (disease specific)
         */
        val summary: CodeableConcept?

        /**
         * Formal record of assessment
         */
        val assessment: List<Reference>

        /**
         * Kind of staging
         */
        val type: CodeableConcept?
    }

    /**
     * Representation of a condition's evidence.
     */
    interface Evidence : EHRElement {
        /**
         * Manifestation/symptom
         */
        val code: List<CodeableConcept>

        /**
         * Supporting information found elsewhere
         */
        val detail: List<Reference>
    }
}
