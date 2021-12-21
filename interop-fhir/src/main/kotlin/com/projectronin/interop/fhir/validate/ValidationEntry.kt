package com.projectronin.interop.fhir.validate

/**
 * A specific FHIR validation entry.
 */
data class ValidationEntry(val severity: String, val location: String, val details: String)
