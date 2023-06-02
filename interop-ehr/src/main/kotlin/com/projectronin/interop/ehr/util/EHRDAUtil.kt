package com.projectronin.interop.ehr.util

import com.projectronin.ehr.dataauthority.models.Identifier
import com.projectronin.ehr.dataauthority.models.IdentifierSearchResponse
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.fhir.r4.CodeSystem

/**
 * Convenience functions for the return object from Ehr Data Authority's "getResourceIdentifiers" API.
 * Could potentially be moved to EHRDA repo in the future.
 */

/**
 * Finds the FHIR ID among the list of identifiers
 * @throws VendorIdentifierNotFoundException
 */
fun IdentifierSearchResponse.getFHIRId(): String {
    if (this.foundResources.size > 1) {
        throw VendorIdentifierNotFoundException("Single resource could not be matched for $searchedIdentifier")
    }
    return foundResources.first().identifiers
        .firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri.value }?.value
        ?: throw VendorIdentifierNotFoundException("No FHIR ID found for resource in identifier list")
}

/**
 * Associates the FHIR ID to the original identifier used for searching.
 */
fun List<IdentifierSearchResponse>.associateFHIRId(): Map<Identifier, String> =
    this.filter { it.foundResources.isNotEmpty() }.associate { it.searchedIdentifier to it.getFHIRId() }
