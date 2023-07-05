package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyWeight
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class bodyWeight(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) : RoninBodyWeight(
    normalizer,
    localizer,
    registryClient
) {
    override fun qualifyingCodes(): List<Coding> = possibleBodyWeightCodes
}
class RoninBodyWeightGeneratorTest {
    private lateinit var roninBodyWeight: RoninBodyWeight
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
        registry = mockk<NormalizationRegistryClient> {}
        roninBodyWeight = bodyWeight(normalizer, localizer, registry)
    }

    @Test
    fun `example use for roninObservationBodyWeight`() {
        // Create BodyWeight Obs with attributes you need, provide the tenant
        val roninObsBodyWeight = rcdmObservationBodyWeight("test") {
            // if you want to test for a specific status
            status of Code("random-status")
            // test for a new or different code
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        code of Code("89543-3")
                        display of "Laboratory ask at order entry panel"
                    }
                )
                text of "Laboratory ask at order entry panel" // text is kept if provided otherwise only a code.coding is generated
            }
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBodyWeightJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBodyWeight)

        // Uncomment to take a peek at the JSON
        // println(roninObsBodyWeightJSON)
        assertNotNull(roninObsBodyWeightJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationBodyWeight - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsBodyWeight = rcdmPatient.rcdmObservationBodyWeight {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBodyWeightJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBodyWeight)

        // Uncomment to take a peek at the JSON
        // println(roninObsBodyWeightJSON)
        assertNotNull(roninObsBodyWeightJSON)
        assertNotNull(roninObsBodyWeight.meta)
        assertEquals(
            roninObsBodyWeight.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BODY_WEIGHT.value
        )
        assertNotNull(roninObsBodyWeight.status)
        assertEquals(1, roninObsBodyWeight.category.size)
        assertNotNull(roninObsBodyWeight.code)
        assertNotNull(roninObsBodyWeight.subject)
        assertNotNull(roninObsBodyWeight.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBodyWeight.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBodyWeight.category[0].coding[0].system)
        assertNotNull(roninObsBodyWeight.status)
        assertNotNull(roninObsBodyWeight.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsBodyWeight.id)
        val patientFHIRId = roninObsBodyWeight.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant = roninObsBodyWeight.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsBodyWeight.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationBodyWeight Observation`() {
        val roninObsWeight = rcdmObservationBodyWeight("test") {}
        assertNotNull(roninObsWeight.id)
        assertNotNull(roninObsWeight.meta)
        assertEquals(
            roninObsWeight.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BODY_WEIGHT.value
        )
        assertNull(roninObsWeight.implicitRules)
        assertNull(roninObsWeight.language)
        assertNull(roninObsWeight.text)
        assertEquals(0, roninObsWeight.contained.size)
        assertEquals(1, roninObsWeight.extension.size)
        assertEquals(0, roninObsWeight.modifierExtension.size)
        assertTrue(roninObsWeight.identifier.size >= 3)
        assertTrue(roninObsWeight.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsWeight.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsWeight.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsWeight.status)
        assertEquals(1, roninObsWeight.category.size)
        assertNotNull(roninObsWeight.code)
        assertNotNull(roninObsWeight.subject)
        assertNotNull(roninObsWeight.subject?.type?.extension)
        assertEquals("vital-signs", roninObsWeight.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsWeight.category[0].coding[0].system)
        assertNotNull(roninObsWeight.status)
        assertNotNull(roninObsWeight.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validation for roninObservationBodyWeight`() {
        val roninObsWeight = rcdmObservationBodyWeight("test") {}
        val validation = roninBodyWeight.validate(roninObsWeight, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `validation for roninObservationBodyWeight with existing identifier`() {
        val roninObsWeight = rcdmObservationBodyWeight("test") {
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
        }
        val validation = roninBodyWeight.validate(roninObsWeight, null)
        val errors = validation.hasErrors()
        assertEquals(errors, false)
        assertNotNull(roninObsWeight.meta)
        assertNotNull(roninObsWeight.identifier)
        assertEquals(4, roninObsWeight.identifier.size)
        assertNotNull(roninObsWeight.status)
        assertNotNull(roninObsWeight.code)
        assertNotNull(roninObsWeight.subject)
    }

    @Test
    fun `validation for roninObservationBodyWeight with code`() {
        val roninObsWeight = rcdmObservationBodyWeight("test") {
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        version of "2.74"
                        code of Code("3141-9")
                        display of "Body weight Measured"
                    }
                )
            }
        }
        val validation = roninBodyWeight.validate(roninObsWeight, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `validation for roninObservationBodyWeight fails with bad code`() {
        val roninObsWeight = rcdmObservationBodyWeight("test") {
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "system"
                        version of "version"
                        code of Code("code")
                        display of "display"
                    }
                )
            }
        }
        val validation = roninBodyWeight.validate(roninObsWeight, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals(validation.issues()[0].code, "RONIN_OBS_003")
    }
}
