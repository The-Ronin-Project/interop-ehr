package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.ContactPoint
import com.projectronin.interop.ehr.model.PractitionerRole
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.base.FHIRResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.model.helper.fhirElementList

class EpicPractitionerRole(override val raw: String) : FHIRResource(raw), PractitionerRole {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.PRACTITIONER_ROLE

    override val id: String by lazy {
        jsonObject.string("id")!!
    }

    override val active: Boolean? by lazy {
        jsonObject.boolean("active")
    }

    override val practitioner: Reference? by lazy {
        jsonObject.obj("practitioner")?.let { EpicReference(it.toJsonString()) }
    }

    override val code: List<CodeableConcept> by lazy {
        jsonObject.fhirElementList("code", ::EpicCodeableConcept)
    }

    override val location: List<Reference> by lazy {
        jsonObject.fhirElementList("location", ::EpicReference)
    }

    override val specialty: List<CodeableConcept> by lazy {
        jsonObject.fhirElementList("specialty", ::EpicCodeableConcept)
    }

    override val telecom: List<ContactPoint> by lazy {
        jsonObject.fhirElementList("telecom", ::EpicContactPoint)
    }
}
