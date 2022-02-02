package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.ContactPoint
import com.projectronin.interop.ehr.model.PractitionerRole
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.resource.PractitionerRole as R4PractitionerRole

class EpicPractitionerRole(override val resource: R4PractitionerRole) : JSONResource(resource), PractitionerRole {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.PRACTITIONER_ROLE

    override val id: String = resource.id!!.value
    override val active: Boolean? = resource.active

    override val practitioner: Reference? by lazy {
        resource.practitioner?.let { EpicReference(it) }
    }

    override val code: List<CodeableConcept> by lazy {
        resource.code.map(::EpicCodeableConcept)
    }

    override val location: List<Reference> by lazy {
        resource.location.map(::EpicReference)
    }

    override val specialty: List<CodeableConcept> by lazy {
        resource.specialty.map(::EpicCodeableConcept)
    }

    override val telecom: List<ContactPoint> by lazy {
        resource.telecom.map(::EpicContactPoint)
    }
}
