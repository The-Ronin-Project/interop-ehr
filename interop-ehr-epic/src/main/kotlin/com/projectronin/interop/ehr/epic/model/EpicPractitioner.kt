package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.ContactPoint
import com.projectronin.interop.ehr.model.HumanName
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Practitioner
import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.resource.Practitioner as R4Practitioner

class EpicPractitioner(override val resource: R4Practitioner) : JSONResource(resource), Practitioner {
    override val dataSource: DataSource = DataSource.FHIR_R4
    override val resourceType: ResourceType = ResourceType.PRACTITIONER

    override val id: String = resource.id!!.value
    override val active: Boolean? = resource.active
    override val gender: AdministrativeGender? = resource.gender

    override val identifier: List<Identifier> by lazy {
        resource.identifier.map(::EpicIdentifier)
    }

    override val communication: List<CodeableConcept> by lazy {
        resource.communication.map(::EpicCodeableConcept)
    }

    override val name: List<HumanName> by lazy {
        resource.name.map(::EpicHumanName)
    }

    override val telecom: List<ContactPoint> by lazy {
        resource.telecom.map(::EpicContactPoint)
    }
}
