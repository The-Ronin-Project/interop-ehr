package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.exception.UnsupportedDynamicValueTypeException
import com.projectronin.interop.ehr.model.Annotation
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.ehr.model.Condition.Abatement
import com.projectronin.interop.ehr.model.Condition.AgeAbatement
import com.projectronin.interop.ehr.model.Condition.AgeOnset
import com.projectronin.interop.ehr.model.Condition.DateTimeAbatement
import com.projectronin.interop.ehr.model.Condition.DateTimeOnset
import com.projectronin.interop.ehr.model.Condition.Evidence
import com.projectronin.interop.ehr.model.Condition.Onset
import com.projectronin.interop.ehr.model.Condition.PeriodAbatement
import com.projectronin.interop.ehr.model.Condition.PeriodOnset
import com.projectronin.interop.ehr.model.Condition.RangeAbatement
import com.projectronin.interop.ehr.model.Condition.RangeOnset
import com.projectronin.interop.ehr.model.Condition.Stage
import com.projectronin.interop.ehr.model.Condition.StringAbatement
import com.projectronin.interop.ehr.model.Condition.StringOnset
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.Age
import com.projectronin.interop.fhir.r4.datatype.ConditionEvidence
import com.projectronin.interop.fhir.r4.datatype.ConditionStage
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Range
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.resource.Condition as R4Condition

/**
 * Epic implementation of [Condition]
 */
class EpicCondition(override val resource: R4Condition) : JSONResource(resource), Condition {
    override val dataSource: DataSource = DataSource.FHIR_R4
    override val resourceType: ResourceType = ResourceType.CONDITION

    override val id: String = resource.id!!.value
    override val recordedDate: String? = resource.recordedDate?.value

    override val identifier: List<Identifier> by lazy {
        resource.identifier.map(::EpicIdentifier)
    }

    override val clinicalStatus: CodeableConcept? by lazy {
        resource.clinicalStatus?.let { EpicCodeableConcept(it) }
    }

    override val verificationStatus: CodeableConcept? by lazy {
        resource.verificationStatus?.let { EpicCodeableConcept(it) }
    }

    override val category: List<CodeableConcept> by lazy {
        resource.category.map(::EpicCodeableConcept)
    }

    override val severity: CodeableConcept? by lazy {
        resource.severity?.let { EpicCodeableConcept(it) }
    }

    override val code: CodeableConcept? by lazy {
        resource.code?.let { EpicCodeableConcept(it) }
    }

    override val bodySite: List<CodeableConcept> by lazy {
        resource.bodySite.map(::EpicCodeableConcept)
    }
    override val subject: Reference by lazy {
        EpicReference(resource.subject)
    }

    override val encounter: Reference? by lazy {
        resource.encounter?.let { EpicReference(it) }
    }

    override val onset: Onset<out Any>? by lazy {
        resource.onset?.let {
            when (it.type) {
                DynamicValueType.DATE_TIME -> DateTimeOnset((it.value as DateTime).value)
                DynamicValueType.AGE -> AgeOnset(EpicAge(it.value as Age))
                DynamicValueType.PERIOD -> PeriodOnset(EpicPeriod(it.value as Period))
                DynamicValueType.RANGE -> RangeOnset(EpicRange(it.value as Range))
                DynamicValueType.STRING -> StringOnset(it.value as String)
                else -> throw UnsupportedDynamicValueTypeException("${it.type} is not a supported type for condition onset")
            }
        }
    }

    override val abatement: Abatement<out Any>? by lazy {
        resource.abatement?.let {
            when (it.type) {
                DynamicValueType.DATE_TIME -> DateTimeAbatement((it.value as DateTime).value)
                DynamicValueType.AGE -> AgeAbatement(EpicAge(it.value as Age))
                DynamicValueType.PERIOD -> PeriodAbatement(EpicPeriod(it.value as Period))
                DynamicValueType.RANGE -> RangeAbatement(EpicRange(it.value as Range))
                DynamicValueType.STRING -> StringAbatement(it.value as String)
                else -> throw UnsupportedDynamicValueTypeException("${it.type} is not a supported type for condition abatement")
            }
        }
    }

    override val recorder: Reference? by lazy {
        resource.recorder?.let { EpicReference(it) }
    }

    override val asserter: Reference? by lazy {
        resource.asserter?.let { EpicReference(it) }
    }

    override val stage: List<Stage> by lazy {
        resource.stage.map(::EpicStage)
    }

    override val evidence: List<Evidence> by lazy {
        resource.evidence.map(::EpicEvidence)
    }

    override val note: List<Annotation> by lazy {
        resource.note.map(::EpicAnnotation)
    }

    /**
     * Epic implementation of [Stage]
     */
    class EpicStage(override val element: ConditionStage) : JSONElement(element), Stage {
        override val summary: CodeableConcept? by lazy {
            element.summary?.let { EpicCodeableConcept(it) }
        }

        override val assessment: List<Reference> by lazy {
            element.assessment.map(::EpicReference)
        }

        override val type: CodeableConcept? by lazy {
            element.type?.let { EpicCodeableConcept(it) }
        }
    }

    /**
     * Epic implementation of [Evidence]
     */
    class EpicEvidence(override val element: ConditionEvidence) : JSONElement(element), Evidence {
        override val code: List<CodeableConcept> by lazy {
            element.code.map(::EpicCodeableConcept)
        }

        override val detail: List<Reference> by lazy {
            element.detail.map(::EpicReference)
        }
    }
}
