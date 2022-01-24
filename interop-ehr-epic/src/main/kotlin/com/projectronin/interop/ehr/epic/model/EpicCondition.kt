package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.base.FHIRResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.model.helper.fhirElementList

class EpicCondition(override val raw: String) : FHIRResource(raw), Condition {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.CONDITION

    override val id: String by lazy {
        jsonObject.string("id")!!
    }

    override val identifier: List<Identifier> by lazy {
        jsonObject.fhirElementList("identifier", ::EpicIdentifier)
    }

    override val clinicalStatus: CodeableConcept? by lazy {
        jsonObject.obj("clinicalStatus")?.let { EpicCodeableConcept(it.toJsonString()) }
    }

    override val verificationStatus: CodeableConcept? by lazy {
        jsonObject.obj("verificationStatus")?.let { EpicCodeableConcept(it.toJsonString()) }
    }

    override val category: List<CodeableConcept> by lazy {
        jsonObject.fhirElementList("category", ::EpicCodeableConcept)
    }

    override val code: CodeableConcept? by lazy {
        jsonObject.obj("code")?.let { EpicCodeableConcept(it.toJsonString()) }
    }
}
