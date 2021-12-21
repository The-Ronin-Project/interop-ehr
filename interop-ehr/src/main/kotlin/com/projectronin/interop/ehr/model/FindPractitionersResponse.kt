package com.projectronin.interop.ehr.model

/**
 * Response from a request to find practitioners.
 */
interface FindPractitionersResponse {
    /**
     * The [Bundle] of [PractitionerRole]s in the response, if appropriate for linking between the search criteria and the practitioners.
     */
    val practitionerRoles: Bundle<PractitionerRole>?

    /**
     * The [Bundle] of [Practitioner]s found based off the request.
     */
    val practitioners: Bundle<Practitioner>
}
