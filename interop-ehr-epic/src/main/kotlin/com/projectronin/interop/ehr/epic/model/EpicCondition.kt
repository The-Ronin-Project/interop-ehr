package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.resource.Condition as R4Condition

class EpicCondition(override val resource: R4Condition) : JSONResource(resource), Condition {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.CONDITION

    override val id: String = resource.id!!.value

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

    override val code: CodeableConcept? by lazy {
        resource.code?.let { EpicCodeableConcept(it) }
    }
}
