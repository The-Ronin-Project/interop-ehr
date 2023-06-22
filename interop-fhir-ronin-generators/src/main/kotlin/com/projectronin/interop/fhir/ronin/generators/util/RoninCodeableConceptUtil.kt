package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding

fun generateCodeableConcept(codeableconcept: CodeableConcept, possibleCodes: Coding): CodeableConcept {
    if (codeableconcept.coding.isEmpty()) {
        return codeableConcept {
            coding of listOf(possibleCodes)
            text of codeableconcept.text
        }
    }
    return codeableconcept
}
