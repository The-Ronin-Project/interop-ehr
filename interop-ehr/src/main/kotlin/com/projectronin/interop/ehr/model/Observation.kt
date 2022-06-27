package com.projectronin.interop.ehr.model

/**
 * Representation of an Observation
 */
interface Observation : EHRResource {
    /**
     * Logical ID of this observation.
     */
    val id: String

    /**
     * The procedure or service this observation is associated with.
     */
    val basedOn: List<Reference>?

    /**
     * Code of the Observation.
     */
    val code: CodeableConcept

    /**
     * The Observation type.
     */
    val category: List<CodeableConcept>

    /**
     * The reason why normally expected content of the data element is missing.
     */
    val dataAbsentReason: CodeableConcept?

    /**
     * The method or isolate level Observation FHIR ID.
     */
    val derivedFrom: List<Reference>?

    /**
     * The prioritized instant. This is the instant the results were last updated.
     */
    val effective: Effective<out Any>?

    sealed interface Effective<T> {
        val value: T
    }

    class EffectiveDateTime(override val value: String) : Effective<String>

    /**
     * The Encounter FHIR ID linked to the order.
     */
    val encounter: Reference?

    /**
     * Member references
     */
    val hasMember: List<Reference>?

    /**
     * The level of abnormality.
     */
    val interpretation: List<CodeableConcept>?

    /**
     * The observation instant.
     */
    val issued: String?

    /**
     * SNOMED code of procedure
     */
    val method: CodeableConcept?

    /**
     * Comments about the result.
     */
    val note: List<Annotation>?

    /**
     * A reference to the resulting agency's director's Practitioner resource.
     */
    val performer: List<Reference>?

    /**
     * The reference range for the result, if applicable.
     */
    val referenceRange: List<ReferenceRange>?

    /**
     * A reference to the Specimen resource.
     */
    val specimen: Reference?

    /**
     * The status of the lab order.
     */
    val status: String

    /**
     * The Patient FHIR ID linked to the order.
     */
    val subject: Reference

    val value: Value<out Any>?

    sealed interface Value<T> {
        val value: T
    }

    class ValueCodeableConcept(override val value: CodeableConcept) : Value<CodeableConcept>

    class ValueQuantity(override val value: SimpleQuantity) : Value<SimpleQuantity>

    class ValueRange(override val value: Range) : Value<Range>

    class ValueRatio(override val value: Ratio) : Value<Ratio>

    class ValueString(override val value: String) : Value<String>
}
