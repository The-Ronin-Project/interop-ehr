package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.Address
import com.projectronin.interop.ehr.model.ContactPoint
import com.projectronin.interop.ehr.model.HumanName
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.ehr.model.base.FHIRResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.model.helper.enum
import com.projectronin.interop.ehr.model.helper.fhirElementList
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

class EpicPatient(override val raw: String) : FHIRResource(raw), Patient {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.PATIENT

    override val id: String by lazy {
        jsonObject.string("id")!!
    }

    override val identifier: List<Identifier> by lazy {
        jsonObject.fhirElementList("identifier", ::EpicIdentifier)
    }

    override val name: List<HumanName> by lazy {
        jsonObject.fhirElementList("name", ::EpicHumanName)
    }

    override val gender: AdministrativeGender? by lazy {
        jsonObject.enum<AdministrativeGender>("gender")
    }

    override val birthDate: String? by lazy {
        jsonObject.string("birthDate")
    }

    override val telecom: List<ContactPoint> by lazy {
        jsonObject.fhirElementList("telecom", ::EpicContactPoint)
    }

    override val address: List<Address> by lazy {
        jsonObject.fhirElementList("address", ::EpicAddress)
    }
}
