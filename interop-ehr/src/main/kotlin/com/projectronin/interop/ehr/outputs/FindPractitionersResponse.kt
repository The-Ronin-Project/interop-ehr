package com.projectronin.interop.ehr.outputs

import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.resource.Resource

class FindPractitionersResponse(val resource: Bundle) {

    /**
     *  Resources is a list of PractitionerRole, Practitioner, and Location resources.
     *  This simplifies merging bundles later when we have to deal with paging in FHIR results.
     */
    val resources: List<Resource<*>> by lazy {
        resource.entry.mapNotNull { it.resource }
    }

    val practitionerRoles: List<PractitionerRole> by lazy {
        resources.filterIsInstance<PractitionerRole>()
    }

    val practitioners: List<Practitioner> by lazy {
        resources.filterIsInstance<Practitioner>().toSet().toList() // de-duplicate
    }

    val locations: List<Location> by lazy {
        resources.filterIsInstance<Location>().toSet().toList()
    }
}
