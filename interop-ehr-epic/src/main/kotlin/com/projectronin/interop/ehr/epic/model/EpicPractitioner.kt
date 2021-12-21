package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.ContactPoint
import com.projectronin.interop.ehr.model.HumanName
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Practitioner
import com.projectronin.interop.ehr.model.base.FHIRResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.model.helper.enum
import com.projectronin.interop.ehr.model.helper.fhirElementList
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

class EpicPractitioner(override val raw: String) : FHIRResource(raw), Practitioner {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.PRACTITIONER

    override val id: String by lazy {
        jsonObject.string("id")!!
    }

    override val identifier: List<Identifier> by lazy {
        jsonObject.fhirElementList("identifier", ::EpicIdentifier)
    }

    override val active: Boolean? by lazy {
        jsonObject.boolean("active")
    }

    override val communication: List<CodeableConcept> by lazy {
        jsonObject.fhirElementList("communication", ::EpicCodeableConcept)
    }

    override val gender: AdministrativeGender? by lazy {
        jsonObject.enum<AdministrativeGender>("gender")
    }

    override val name: List<HumanName> by lazy {
        jsonObject.fhirElementList("name", ::EpicHumanName)
    }

    override val qualification: List<CodeableConcept> by lazy {
        jsonObject.fhirElementList("qualification", ::EpicCodeableConcept)
    }

    override val telecom: List<ContactPoint> by lazy {
        jsonObject.fhirElementList("telecom", ::EpicContactPoint)
    }
}
