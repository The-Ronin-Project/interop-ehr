package com.projectronin.interop.ehr.inputs

/**
 * Represents a FHIR [search token](https://www.hl7.org/fhir/search.html#token) in system|code or code format.
 */
data class FHIRSearchToken(
    var system: String? = null,
    val code: String
) {
    init {
        if (code.isEmpty()) {
            throw IllegalArgumentException("A FHIR search token requires a code")
        }
    }
    fun toParam(): String {
        if (system.isNullOrEmpty()) {
            return code
        }
        return "$system|$code"
    }
}
