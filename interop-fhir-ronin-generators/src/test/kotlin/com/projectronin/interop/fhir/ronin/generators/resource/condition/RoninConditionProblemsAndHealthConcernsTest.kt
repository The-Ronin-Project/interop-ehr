package com.projectronin.interop.fhir.ronin.generators.resource.condition

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.util.conditionCodeExtension
import com.projectronin.interop.fhir.ronin.generators.util.possibleConditionCodes
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.condition.RoninConditionProblemsAndHealthConcerns
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninConditionProblemsAndHealthConcernsTest {
    private lateinit var roninConditionProblemsAndHealthConcerns: RoninConditionProblemsAndHealthConcerns
    private lateinit var registry: NormalizationRegistryClient
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @BeforeEach
    fun setup() {
        val normalizer: Normalizer = mockk {
            every { normalize(any(), tenant) } answers { firstArg() }
        }
        val localizer: Localizer = mockk {
            every { localize(any(), tenant) } answers { firstArg() }
        }
        registry = mockk<NormalizationRegistryClient> {
            every {
                getRequiredValueSet("Condition.code", RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS.value)
            } returns possibleConditionCodes
        }

        roninConditionProblemsAndHealthConcerns = RoninConditionProblemsAndHealthConcerns(normalizer, localizer)
    }

    @Test
    fun `generates basic ronin condition problems and health concerns`() {
        val roninCondition = rcdmConditionProblemsAndHealthConcerns("test") { }
        val qualified = roninConditionProblemsAndHealthConcerns.qualifies(roninCondition)
        val validate = roninConditionProblemsAndHealthConcerns.validate(roninCondition).hasErrors()
        assertEquals(roninCondition.code?.coding?.size, 1)
        assertFalse(validate)
        assertTrue(qualified)
    }

    @Test
    fun `generates ronin condition problems and health concerns with input parameters`() {
        val roninCondition = rcdmConditionProblemsAndHealthConcerns("test") {
            id of Id("12345")
            extension of listOf(conditionCodeExtension)
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
            category of listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("potatos")
                        )
                    )
                )
            )
            subject of Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Condition", extension = dataAuthorityExtension)
            )
        }
        val qualified = roninConditionProblemsAndHealthConcerns.qualifies(roninCondition)
        val validate = roninConditionProblemsAndHealthConcerns.validate(roninCondition).hasErrors()
        assertFalse(validate)
        assertTrue(qualified)
    }

    @Test
    fun `generates ronin condition problems and health concerns with bad input code - fails validation`() {
        val roninCondition = rcdmConditionProblemsAndHealthConcerns("test") {
            meta of rcdmMeta(RoninProfile.CONDITION_PROBLEMS_CONCERNS, "test") {
                code of codeableConcept {
                    coding of listOf(
                        coding {
                            system of "not valid system"
                            version of "1"
                            code of Code("bad code")
                        }
                    )
                }
            }
        }
        val qualified = roninConditionProblemsAndHealthConcerns.qualifies(roninCondition)
        val validate = roninConditionProblemsAndHealthConcerns.validate(roninCondition).hasErrors()
        assertTrue(validate)
        assertTrue(qualified)
    }
}
