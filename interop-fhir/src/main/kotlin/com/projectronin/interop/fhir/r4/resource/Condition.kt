package com.projectronin.interop.fhir.r4.resource

import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.ConditionDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.ConditionSerializer
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ConditionEvidence
import com.projectronin.interop.fhir.r4.datatype.ConditionStage
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.base.BaseCondition

/**
 * A clinical condition, problem, diagnosis, or other event, situation, issue, or clinical concept that has risen to a level of concern.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/condition.html)
 */
@JsonDeserialize(using = ConditionDeserializer::class)
@JsonSerialize(using = ConditionSerializer::class)
@JsonTypeName("Condition")
data class Condition(
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    override val text: Narrative? = null,
    override val contained: List<ContainedResource> = listOf(),
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    override val identifier: List<Identifier> = listOf(),
    override val clinicalStatus: CodeableConcept? = null,
    override val verificationStatus: CodeableConcept? = null,
    override val category: List<CodeableConcept> = listOf(),
    override val severity: CodeableConcept? = null,
    override val code: CodeableConcept? = null,
    override val bodySite: List<CodeableConcept> = listOf(),
    override val subject: Reference,
    override val encounter: Reference? = null,
    override val onset: DynamicValue<Any>? = null,
    override val abatement: DynamicValue<Any>? = null,
    override val recordedDate: DateTime? = null,
    override val recorder: Reference? = null,
    override val asserter: Reference? = null,
    override val stage: List<ConditionStage> = listOf(),
    override val evidence: List<ConditionEvidence> = listOf(),
    override val note: List<Annotation> = listOf()
) : DomainResource, BaseCondition() {
    init {
        validate()
    }
}
