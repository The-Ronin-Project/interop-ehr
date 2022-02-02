package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.util.convertResources
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.resource.Bundle

class EpicPractitionerRoleBundle(override val resource: Bundle) : EpicFHIRBundle<EpicPractitionerRole>(resource) {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resources: List<EpicPractitionerRole> by lazy {
        resource.convertResources(::EpicPractitionerRole)
    }
}
