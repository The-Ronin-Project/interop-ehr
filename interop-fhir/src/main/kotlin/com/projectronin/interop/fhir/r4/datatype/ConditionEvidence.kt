package com.projectronin.interop.fhir.r4.datatype

/**
 * 	Supporting evidence
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/condition-definitions.html#Condition.evidence)
 */
data class ConditionEvidence(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val code: List<CodeableConcept> = listOf(),
    val detail: List<Reference> = listOf()
) : BackboneElement {
    init {
        require(code.isNotEmpty() || detail.isNotEmpty()) { "evidence SHALL have code or details" }
    }
}
