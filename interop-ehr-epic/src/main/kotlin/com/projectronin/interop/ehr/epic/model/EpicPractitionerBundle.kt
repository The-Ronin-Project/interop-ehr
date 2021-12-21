package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.base.FHIRBundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.model.helper.fhirElementList
import com.projectronin.interop.ehr.model.helper.fhirResourceList

class EpicPractitionerBundle(override val raw: String) : FHIRBundle<EpicPractitioner>(raw) {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.BUNDLE

    override val links: List<Link> by lazy {
        jsonObject.fhirElementList("link", ::EpicLink)
    }

    override val resources: List<EpicPractitioner> by lazy {
        // Older versions of this API in epic can return duplicate providers
        jsonObject.fhirResourceList("entry", "Practitioner", ::EpicPractitioner).toSet().toList()
    }
}
