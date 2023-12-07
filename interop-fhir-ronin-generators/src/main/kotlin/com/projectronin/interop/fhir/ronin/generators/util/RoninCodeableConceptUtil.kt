package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding

/**
 * If the input [codeableconcept] has a non-null coding list, return
 * [codeableconcept]. If the input [codeableconcept] is null, or non-null with
 * an empty coding list, use [possibleCoding] as a coding list and use any text.
 */
fun generateCodeableConcept(
    codeableconcept: CodeableConcept?,
    possibleCoding: Coding,
): CodeableConcept {
    return when {
        codeableconcept == null -> {
            codeableConcept {
                coding of listOf(possibleCoding)
            }
        }
        codeableconcept.coding.isEmpty() -> {
            codeableConcept {
                coding of listOf(possibleCoding)
                text of codeableconcept.text
            }
        }
        else -> codeableconcept
    }
}

/**
 * generate a CodeableConcept list that is 1..* in RCDM. If the input
 * [codeableConcepts] is empty, or all entries have empty coding lists,
 * generate a list of one CodeableConcept using [possibleCoding]
 */
fun generateRequiredCodeableConceptList(
    codeableConcepts: List<CodeableConcept>,
    possibleCoding: Coding,
): List<CodeableConcept> {
    val generated = codeableConcepts.filter { it.coding.isNotEmpty() }
    return generated.ifEmpty {
        listOf(
            codeableConcept {
                coding of listOf(possibleCoding)
            },
        )
    }
}
