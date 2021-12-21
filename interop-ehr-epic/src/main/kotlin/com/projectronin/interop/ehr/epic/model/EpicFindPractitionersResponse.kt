package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.FindPractitionersResponse
import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.base.FHIRBundle
import com.projectronin.interop.ehr.model.base.FHIRResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.model.helper.fhirElementList
import com.projectronin.interop.ehr.model.helper.fhirResourceList

class EpicFindPractitionersResponse(override val raw: String) : FHIRBundle<FHIRResource>(raw), FindPractitionersResponse {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.BUNDLE

    override val links: List<Link> by lazy {
        jsonObject.fhirElementList("link", ::EpicLink)
    }

    // Resources is a list of both PractitionerRole and Practitioner resources.  This simplifies merging bundles later
    // when we have to deal with paging in FHIR results.
    override val resources: List<FHIRResource> by lazy {
        jsonObject.fhirResourceList("entry", "PractitionerRole", ::EpicPractitionerRole) +
            jsonObject.fhirResourceList("entry", "Practitioner", ::EpicPractitioner)
    }

    override val practitionerRoles: FHIRBundle<EpicPractitionerRole> by lazy {
        EpicPractitionerRoleBundle(raw)
    }

    override val practitioners: FHIRBundle<EpicPractitioner> by lazy {
        EpicPractitionerBundle(raw)
    }
}
