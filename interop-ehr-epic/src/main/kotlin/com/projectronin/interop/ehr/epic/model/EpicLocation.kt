package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.Address
import com.projectronin.interop.ehr.model.ContactPoint
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Location
import com.projectronin.interop.ehr.model.Reference
import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.LocationMode
import com.projectronin.interop.fhir.r4.resource.Location as R4Location

class EpicLocation(override val resource: R4Location) : JSONResource(resource), Location {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.LOCATION

    override val id: String = resource.id!!.value

    override val identifier: List<Identifier> by lazy {
        resource.identifier.map(::EpicIdentifier)
    }

    override val name: String? = resource.name

    override val mode: LocationMode? = resource.mode

    override val address: Address? by lazy {
        resource.address?.let { EpicAddress(it) }
    }

    override val telecom: List<ContactPoint> by lazy {
        resource.telecom.map(::EpicContactPoint)
    }

    override val managingOrganization: Reference? by lazy {
        resource.managingOrganization?.let { EpicReference(it) }
    }

    override val partOf: Reference? by lazy {
        resource.partOf?.let { EpicReference(it) }
    }
}
