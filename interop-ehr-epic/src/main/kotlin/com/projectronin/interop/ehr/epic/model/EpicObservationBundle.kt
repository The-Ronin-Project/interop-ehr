package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.util.convertResources
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.resource.Bundle

class EpicObservationBundle(override val resource: Bundle) : EpicFHIRBundle<EpicObservation>(resource) {
    override val dataSource: DataSource = DataSource.FHIR_R4

    override val resources: List<EpicObservation> by lazy {
        resource.convertResources(::EpicObservation)
    }
}
