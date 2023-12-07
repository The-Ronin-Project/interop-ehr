package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id

/**
 * Represents the possible identifier values for a FHIR resource, including its [id] and [identifiers]
 */
data class FHIRIdentifiers(
    val id: Id,
    val identifiers: List<Identifier>,
)
