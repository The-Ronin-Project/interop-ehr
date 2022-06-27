package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.exception.UnsupportedDynamicValueTypeException
import com.projectronin.interop.ehr.model.Observation
import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Range
import com.projectronin.interop.fhir.r4.datatype.Ratio
import com.projectronin.interop.fhir.r4.datatype.SimpleQuantity
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept as R4CodeableConcept
import com.projectronin.interop.fhir.r4.resource.Observation as R4Observation

class EpicObservation(override val resource: R4Observation) : JSONResource(resource), Observation {
    override val dataSource = DataSource.FHIR_R4
    override val resourceType = ResourceType.OBSERVATION
    override val id = resource.id!!.value
    override val basedOn by lazy { resource.basedOn.map(::EpicReference) }

    override val code by lazy {
        EpicCodeableConcept(resource.code)
    }

    override val category by lazy {
        resource.category.map(::EpicCodeableConcept)
    }

    override val dataAbsentReason by lazy {
        resource.dataAbsentReason?.let { EpicCodeableConcept(it) }
    }

    override val derivedFrom by lazy {
        resource.derivedFrom.map(::EpicReference)
    }

    override val effective by lazy {
        resource.effective?.let {
            when (it.type) {
                DynamicValueType.DATE_TIME -> Observation.EffectiveDateTime((it.value as DateTime).value)
                else -> throw UnsupportedDynamicValueTypeException("${it.type} is not a supported type for observation effective.")
            }
        }
    }

    override val encounter by lazy {
        resource.encounter?.let { EpicReference(it) }
    }

    override val hasMember by lazy {
        resource.hasMember.map(::EpicReference)
    }

    override val interpretation by lazy {
        resource.interpretation.map(::EpicCodeableConcept)
    }

    override val issued = resource.issued?.value

    override val method by lazy {
        resource.method?.let { EpicCodeableConcept(it) }
    }

    override val note by lazy {
        resource.note.map(::EpicAnnotation)
    }

    override val performer by lazy {
        resource.performer.map(::EpicReference)
    }

    override val referenceRange by lazy {
        resource.referenceRange.map(::EpicReferenceRange)
    }

    override val specimen by lazy {
        resource.specimen?.let { EpicReference(it) }
    }

    override val status = resource.status.toString()

    override val subject by lazy {
        require(resource.subject != null) { "Epic requires a subject." }
        EpicReference(resource.subject!!)
    }

    override val value by lazy {
        resource.value?.let {
            when (it.type) {
                DynamicValueType.CODEABLE_CONCEPT -> Observation.ValueCodeableConcept(EpicCodeableConcept(it.value as R4CodeableConcept))
                DynamicValueType.RANGE -> Observation.ValueRange(EpicRange(it.value as Range))
                DynamicValueType.QUANTITY -> Observation.ValueQuantity(EpicSimpleQuantity(it.value as SimpleQuantity))
                DynamicValueType.RATIO -> Observation.ValueRatio(EpicRatio(it.value as Ratio))
                DynamicValueType.STRING -> Observation.ValueString(it.value as String)
                else -> throw UnsupportedDynamicValueTypeException("${it.type} is not a supported type for observation value.")
            }
        }
    }
}
