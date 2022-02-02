package com.projectronin.interop.fhir.r4.datatype

/**
 * Stage/grade, usually assessed formally
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/condition-definitions.html#Condition.stage)
 */
data class ConditionStage(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val summary: CodeableConcept? = null,
    val assessment: List<Reference> = listOf(),
    val type: CodeableConcept? = null
) : BackboneElement {
    init {
        require(summary != null || assessment.isNotEmpty()) { "Stage SHALL have summary or assessment" }
    }
}
