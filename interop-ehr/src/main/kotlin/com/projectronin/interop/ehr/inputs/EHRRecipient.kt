package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.datatype.Identifier

/**
 * The [id] recipient of an EHR message, with a list of [Identifier]s
 */
data class EHRRecipient(val id: String, val identifiers: List<Identifier>)
