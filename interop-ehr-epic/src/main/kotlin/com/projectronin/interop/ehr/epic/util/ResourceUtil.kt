package com.projectronin.interop.ehr.epic.util

import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Resource

inline fun <reified R : Resource<R>, J : JSONResource> Bundle.convertResources(converter: (R) -> J): List<J> =
    this.entry.mapNotNull { it.resource }.filterIsInstance<R>().map(converter)
