package com.projectronin.interop.fhir.r4

import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * Enumeration of common extension meanings used in FHIR.
 */
enum class ExtensionMeanings(private val uriString: String) {
    PARTNER_DEPARTMENT("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference");

    val uri = Uri(uriString)
}
