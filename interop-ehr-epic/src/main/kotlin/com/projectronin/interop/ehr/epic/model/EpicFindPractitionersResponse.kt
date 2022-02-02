package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.FindPractitionersResponse
import com.projectronin.interop.ehr.model.base.FHIRBundle
import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.resource.Bundle

class EpicFindPractitionersResponse(override val resource: Bundle) :
    EpicFHIRBundle<JSONResource>(resource),
    FindPractitionersResponse {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    // Resources is a list of both PractitionerRole and Practitioner resources.  This simplifies merging bundles later
    // when we have to deal with paging in FHIR results.
    override val resources: List<JSONResource> by lazy {
        practitioners.resources + practitionerRoles.resources
    }

    override val practitionerRoles: FHIRBundle<EpicPractitionerRole> by lazy {
        EpicPractitionerRoleBundle(resource)
    }

    override val practitioners: FHIRBundle<EpicPractitioner> by lazy {
        EpicPractitionerBundle(resource)
    }
}
