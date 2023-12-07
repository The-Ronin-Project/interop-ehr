package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninObservationCodeUtilTest {
    @Test
    fun `generate code if none is provided`() {
        val code = codeableConcept { listOf(coding {}) }
        val roninCode = generateCodeableConcept(code, possibleCodes.random())
        assertTrue(possibleCodes.contains(roninCode.coding.first()))
    }

    @Test
    fun `keep provided code`() {
        val providedCode =
            codeableConcept {
                coding of
                    listOf(
                        coding {
                            system of "system"
                            version of "version"
                            code of Code("this could be anything")
                        },
                    )
            }
        val roninCode = generateCodeableConcept(providedCode, possibleCodes.random())
        assertEquals(providedCode, roninCode)
    }

    @Test
    fun `generate coding if input is null`() {
        val roninCode = generateCodeableConcept(null, possibleCodes.random())
        assertTrue(possibleCodes.contains(roninCode.coding.first()))
    }

    @Test
    fun `generate coding list if none is provided`() {
        val conceptList = listOf(codeableConcept { listOf(coding {}) })
        val roninCode = generateRequiredCodeableConceptList(conceptList, possibleCodes.random())
        assertTrue(possibleCodes.contains(roninCode.first().coding.first()))
    }

    @Test
    fun `keep provided coding list`() {
        val providedList =
            listOf(
                codeableConcept {
                    coding of
                        listOf(
                            coding {
                                system of "system"
                                version of "version"
                                code of Code("this could be anything")
                            },
                        )
                },
            )
        val roninCode = generateRequiredCodeableConceptList(providedList, possibleCodes.random())
        assertEquals(providedList, roninCode)
    }

    @Test
    fun `generate coding list if input is empty`() {
        val conceptList = emptyList<CodeableConcept>()
        val roninCode = generateRequiredCodeableConceptList(conceptList, possibleCodes.random())
        assertTrue(possibleCodes.contains(roninCode.first().coding.first()))
    }
}

val possibleCodes =
    listOf(
        coding {
            system of "test-system-green"
            version of "version green"
            code of Code("green-green")
            display of "green is green"
        },
        coding {
            system of "system-test-orange"
            version of "version blue"
            code of Code("fake-code")
            display of "oranges are orange"
        },
    )
