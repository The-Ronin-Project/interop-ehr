package com.projectronin.interop.fhir.ronin.generators.resource.condition

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.generators.util.conditionCodeExtension
import com.projectronin.interop.fhir.ronin.generators.util.possibleConditionCodes
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.condition.RoninConditionProblemsAndHealthConcerns
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
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
        // create condition problems and health concerns resource with attributes you need, provide the tenant
        val roninCondition = rcdmConditionProblemsAndHealthConcerns("test") { }
        val qualified = roninConditionProblemsAndHealthConcerns.qualifies(roninCondition)
        val validate = roninConditionProblemsAndHealthConcerns.validate(roninCondition).hasErrors()
        assertEquals(roninCondition.code?.coding?.size, 1)
        assertNotNull(roninCondition.subject)
        assertNotNull(roninCondition.id)
        val patientFHIRId = roninCondition.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant = roninCondition.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninCondition.id?.value.toString())
        assertEquals("test", tenant)
        assertFalse(validate)
        assertTrue(qualified)

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninConditionJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninCondition)
        // Uncomment to take a peek at the JSON
        // println(roninConditionJSON)
        assertNotNull(roninConditionJSON)
    }

    @Test
    fun `generates ronin condition problems and health concerns with input parameters`() {
        // create patient and condition problems and health concerns for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val roninCondition = rcdmPatient.rcdmConditionProblemsAndHealthConcerns {
            // add any attributes you need
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

    @Test
    fun `invalid subject input - fails validation`() {
        val roninCondition = rcdmConditionProblemsAndHealthConcerns("test") {
            subject of rcdmReference("Location", "456")
        }
        val validation = roninConditionProblemsAndHealthConcerns.validate(roninCondition, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals("RONIN_INV_REF_TYPE", validation.issues()[0].code)
        assertEquals("The referenced resource type was not Patient", validation.issues()[0].description)
        assertEquals(LocationContext(element = "Condition", field = "subject"), validation.issues()[0].location)
    }

    @Test
    fun `rcdmConditionProblemsAndHealthConcerns - valid subject input - validate succeeds`() {
        val roninCondition = rcdmConditionProblemsAndHealthConcerns("test") {
            subject of rcdmReference("Patient", "456")
        }
        val validation = roninConditionProblemsAndHealthConcerns.validate(roninCondition, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/456", roninCondition.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmConditionProblemsAndHealthConcerns validates`() {
        val rcdmPatient = rcdmPatient("test") {}
        val roninCondition = rcdmPatient.rcdmConditionProblemsAndHealthConcerns {}
        assertEquals("Patient/${rcdmPatient.id?.value}", roninCondition.subject?.reference?.value)
        val validation = roninConditionProblemsAndHealthConcerns.validate(roninCondition, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `rcdmPatient rcdmConditionProblemsAndHealthConcerns - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val roninCondition = rcdmPatient.rcdmConditionProblemsAndHealthConcerns {
            subject of rcdmReference("Patient", "456")
        }
        val validation = roninConditionProblemsAndHealthConcerns.validate(roninCondition, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/456", roninCondition.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmConditionProblemsAndHealthConcerns - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val roninCondition = rcdmPatient.rcdmConditionProblemsAndHealthConcerns {
            subject of reference("Patient", "456")
        }
        val validation = roninConditionProblemsAndHealthConcerns.validate(roninCondition, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/${rcdmPatient.id?.value}", roninCondition.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmConditionProblemsAndHealthConcerns - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") { id of "99" }
        val roninCondition = rcdmPatient.rcdmConditionProblemsAndHealthConcerns {
            id of "88"
        }
        val validation = roninConditionProblemsAndHealthConcerns.validate(roninCondition, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals(3, roninCondition.identifier.size)
        val values = roninCondition.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains("test".asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", roninCondition.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", roninCondition.subject?.reference?.value)
    }
}
