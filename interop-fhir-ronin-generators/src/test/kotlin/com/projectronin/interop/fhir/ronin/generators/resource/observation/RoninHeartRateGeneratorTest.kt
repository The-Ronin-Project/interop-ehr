package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninHeartRate
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninHeartRateGeneratorTest {
    private lateinit var roninHeartRate: RoninHeartRate
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
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_HEART_RATE.value)
            } returns possibleHeartRateCodes
        }
        roninHeartRate = RoninHeartRate(normalizer, localizer, registry)
    }

    @Test
    fun `example use for roninObservationHeartRate`() {
        // Create HeartRate Obs with attributes you need, provide the tenant
        val roninObsHeartRate = rcdmObservationHeartRate("test") {
            // if you want to test for a specific status
            status of Code("heart-rate-status")
            // test for a new or different code
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        code of Code("89299-2")
                        display of "Heart rate special circumstances"
                    }
                )
                text of "Heart rate special circumstances" // text is kept if provided otherwise only a code.coding is generated
            }
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsHeartRateJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsHeartRate)

        // Uncomment to take a peek at the JSON
        // println(roninObsHeartRateJSON)
        assertNotNull(roninObsHeartRateJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationHeartRate - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsHeartRate = rcdmPatient.rcdmObservationHeartRate {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsHeartRateJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsHeartRate)

        // Uncomment to take a peek at the JSON
        // println(roninObsHeartRateJSON)
        assertNotNull(roninObsHeartRateJSON)
        assertNotNull(roninObsHeartRate.meta)
        assertEquals(
            roninObsHeartRate.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_HEART_RATE.value
        )
        assertNotNull(roninObsHeartRate.status)
        assertEquals(1, roninObsHeartRate.category.size)
        assertNotNull(roninObsHeartRate.code)
        assertNotNull(roninObsHeartRate.subject)
        assertNotNull(roninObsHeartRate.subject?.type?.extension)
        assertEquals("vital-signs", roninObsHeartRate.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsHeartRate.category[0].coding[0].system)
        assertNotNull(roninObsHeartRate.status)
        assertNotNull(roninObsHeartRate.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsHeartRate.id)
        val patientFHIRId =
            roninObsHeartRate.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninObsHeartRate.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsHeartRate.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationHeartRate Observation`() {
        val roninObsHeartRate = rcdmObservationHeartRate("test") {}
        assertNotNull(roninObsHeartRate.id)
        assertNotNull(roninObsHeartRate.meta)
        assertEquals(
            roninObsHeartRate.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_HEART_RATE.value
        )
        assertNull(roninObsHeartRate.implicitRules)
        assertNull(roninObsHeartRate.language)
        assertNull(roninObsHeartRate.text)
        assertEquals(0, roninObsHeartRate.contained.size)
        assertEquals(1, roninObsHeartRate.extension.size)
        assertEquals(0, roninObsHeartRate.modifierExtension.size)
        assertTrue(roninObsHeartRate.identifier.size >= 3)
        assertTrue(roninObsHeartRate.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsHeartRate.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsHeartRate.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsHeartRate.status)
        assertEquals(1, roninObsHeartRate.category.size)
        assertNotNull(roninObsHeartRate.code)
        assertNotNull(roninObsHeartRate.subject)
        assertNotNull(roninObsHeartRate.subject?.type?.extension)
        assertEquals("vital-signs", roninObsHeartRate.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsHeartRate.category[0].coding[0].system)
        assertNotNull(roninObsHeartRate.status)
        assertNotNull(roninObsHeartRate.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for roninObservationHeartRate`() {
        val roninObsHeartRate = rcdmObservationHeartRate("test") {}
        val validation = roninHeartRate.validate(roninObsHeartRate, null)
        validation.alertIfErrors()
    }

    @Test
    fun `validation for roninObservationHeartRate with existing identifier`() {
        val roninObsHeartRate = rcdmObservationHeartRate("test") {
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
        }
        val validation = roninHeartRate.validate(roninObsHeartRate, null)
        validation.alertIfErrors()
        assertNotNull(roninObsHeartRate.meta)
        assertNotNull(roninObsHeartRate.identifier)
        assertEquals(4, roninObsHeartRate.identifier.size)
        assertNotNull(roninObsHeartRate.status)
        assertNotNull(roninObsHeartRate.code)
        assertNotNull(roninObsHeartRate.subject)
    }

    @Test
    fun `validation passed for roninObservationHeartRate with code`() {
        val roninObsHeartRate = rcdmObservationHeartRate("test") {
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        version of "2.74"
                        code of Code("8867-4")
                        display of "Heart rate"
                    }
                )
            }
        }
        val validation = roninHeartRate.validate(roninObsHeartRate, null)
        validation.alertIfErrors()
    }

    @Test
    fun `validation fails for roninObservationHeartRate with bad code`() {
        val roninObsHeartRate = rcdmObservationHeartRate("test") {
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        version of "1000000"
                        code of Code("some code here")
                    }
                )
            }
        }
        val validation = roninHeartRate.validate(roninObsHeartRate, null)
        assertTrue(validation.hasErrors())

        val issueCodes = validation.issues().map { it.code }.toSet()
        assertEquals(setOf("RONIN_OBS_003"), issueCodes)
    }
}
