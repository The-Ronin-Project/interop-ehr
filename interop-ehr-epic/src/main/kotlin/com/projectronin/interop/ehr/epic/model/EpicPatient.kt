package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.Address
import com.projectronin.interop.ehr.model.ContactPoint
import com.projectronin.interop.ehr.model.HumanName
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.resource.Patient as R4Patient

class EpicPatient(override val resource: R4Patient) : JSONResource(resource), Patient {
    override val dataSource: DataSource = DataSource.FHIR_R4
    override val resourceType: ResourceType = ResourceType.PATIENT

    override val id: String = resource.id!!.value
    override val gender: AdministrativeGender? = resource.gender
    override val birthDate: String? = resource.birthDate?.value

    override val identifier: List<Identifier> by lazy {
        resource.identifier.map(::EpicIdentifier)
    }

    override val name: List<HumanName> by lazy {
        resource.name.map(::EpicHumanName)
    }

    override val telecom: List<ContactPoint> by lazy {
        resource.telecom.map(::EpicContactPoint)
    }

    override val address: List<Address> by lazy {
        resource.address.map(::EpicAddress)
    }
}
