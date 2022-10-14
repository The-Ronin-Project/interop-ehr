package com.projectronin.interop.fhir.ronin.code

import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * Access to common Ronin-specific code systems
 */
enum class RoninCodeSystem(uriString: String) {
    TENANT("http://projectronin.com/id/tenantId"),
    MRN("http://projectronin.com/id/mrn"),
    FHIR_ID("http://projectronin.com/id/fhir");

    val uri = Uri(uriString)
}
