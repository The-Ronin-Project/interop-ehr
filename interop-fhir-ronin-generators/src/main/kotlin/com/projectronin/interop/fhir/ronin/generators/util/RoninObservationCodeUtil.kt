package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding

fun generateCode(code: CodeableConcept, possibleCodes: Coding): CodeableConcept {
    if (code.coding.isEmpty()) {
        return codeableConcept {
            coding of listOf(possibleCodes)
            text of code.text
        }
    }
    return code
}
