package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.model.EHRResource
import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.base.FHIRBundle
import com.projectronin.interop.fhir.r4.resource.Bundle

abstract class EpicFHIRBundle<out R : EHRResource>(override val resource: Bundle) : FHIRBundle<R>(resource) {
    override val links: List<Link> by lazy {
        resource.link.map(::EpicLink)
    }
}
