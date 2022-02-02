package com.projectronin.interop.fhir.r4.resource.base

import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ConditionEvidence
import com.projectronin.interop.fhir.r4.datatype.ConditionStage
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource

/**
 * Base class representing a FHIR R4 Condition.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/R4/condition.html)
 */
abstract class BaseCondition {
    val resourceType: String = "Condition"

    abstract val id: Id?
    abstract val meta: Meta?
    abstract val implicitRules: Uri?
    abstract val language: Code?
    abstract val text: Narrative?
    abstract val contained: List<ContainedResource>
    abstract val extension: List<Extension>
    abstract val modifierExtension: List<Extension>
    abstract val identifier: List<Identifier>
    abstract val clinicalStatus: CodeableConcept?
    abstract val verificationStatus: CodeableConcept?
    abstract val category: List<CodeableConcept>
    abstract val severity: CodeableConcept?
    abstract val code: CodeableConcept?
    abstract val bodySite: List<CodeableConcept>
    abstract val subject: Reference
    abstract val encounter: Reference?
    abstract val onset: DynamicValue<Any>?
    abstract val abatement: DynamicValue<Any>?
    abstract val recordedDate: DateTime?
    abstract val recorder: Reference?
    abstract val asserter: Reference?
    abstract val stage: List<ConditionStage>
    abstract val evidence: List<ConditionEvidence>
    abstract val note: List<Annotation>

    companion object {
        val acceptedOnsets = listOf(
            DynamicValueType.DATE_TIME,
            DynamicValueType.AGE,
            DynamicValueType.PERIOD,
            DynamicValueType.RANGE,
            DynamicValueType.STRING
        )
        val acceptedAbatements = listOf(
            DynamicValueType.DATE_TIME,
            DynamicValueType.AGE,
            DynamicValueType.PERIOD,
            DynamicValueType.RANGE,
            DynamicValueType.STRING
        )
    }

    protected fun validate() {
        onset?.let {
            require(acceptedOnsets.contains(it.type)) { "Bad dynamic value indicating condition onset" }
        }
        abatement?.let {
            require(acceptedAbatements.contains(it.type)) { "Bad dynamic value indicating condition abatement" }
        }
    }
}
