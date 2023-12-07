package com.projectronin.interop.ehr.util

import com.projectronin.interop.ehr.inputs.FHIRSearchToken

/**
 * Converts a list of strings in FHIR search token format system|code to a list of [FHIRSearchToken] objects.
 * The strings may simply be code values without being FHIR search tokens.
 */
fun List<String>.toSearchTokens(): List<FHIRSearchToken> {
    return this.map { token ->
        val valueList = token.split("|")
        val systemValue =
            if (valueList.size > 1) {
                valueList[0]
            } else {
                null
            }
        val codeValue =
            if (valueList.size > 1) {
                valueList[1]
            } else {
                token
            }
        FHIRSearchToken(
            system = systemValue,
            code = codeValue,
        )
    }
}

/**
 * Converts a list of [FHIRSearchToken]s into an URL search parameter value with comma separators.
 * When this result is supplied with a URL parameter, it matches on any single value in the token list,
 * a logical "OR" (matches token1 or token2 or token3 in a list of 3 tokens).
 */
fun List<FHIRSearchToken>.toOrParams(): String {
    return this.joinToString(separator = ",") { it.toParam() }
}
