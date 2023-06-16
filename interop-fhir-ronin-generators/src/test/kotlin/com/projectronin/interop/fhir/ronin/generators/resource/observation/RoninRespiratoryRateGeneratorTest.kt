package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninRespiratoryRate
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninRespiratoryRateGeneratorTest {
    private lateinit var roninRespRate: RoninRespiratoryRate
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
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_RESPIRATORY_RATE.value)
            } returns possibleRespiratoryRateCodes
        }
        roninRespRate = RoninRespiratoryRate(normalizer, localizer, registry)
    }

    @Test
    fun `example use for roninObservationRespiratoryRate`() {
        // Create RespiratoryRate Obs with attributes you need, provide the tenant(mda), here "fake-tenant"
        val roninObsRespiratoryRate = rcdmObservationRespiratoryRate("fake-tenant") {
            // if you want to test for a specific status
            status of Code("unknown")
            // test for a new or different code
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        version of "0.01"
                        code of Code("00000-2")
                        display of "Respiratory rate"
                    }
                )
                text of "Respiratory rate" // text is kept if provided otherwise only a code.coding is generated
            }
            // test for a specific subject / patient - here you pass 'type' of PATIENT and 'id' of 678910
            subject of rcdmReference("Patient", "678910")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsRespiratoryRateJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsRespiratoryRate)

        // Uncomment to take a peek at the JSON
        // println(roninObsRespiratoryRateJSON)
        assertNotNull(roninObsRespiratoryRateJSON)
    }

    @Test
    fun `example use for roninObservationRespiratoryRate - missing required fields generated`() {
        // Create RespiratoryRate Obs with attributes you need, provide the tenant(mda), here "fake-tenant"
        val roninObsRespiratoryRate = rcdmObservationRespiratoryRate("fake-tenant") {
            // status, code and category required and will be generated
            // test for a specific subject / patient - here you pass 'type' of PATIENT and 'id' of 678910
            subject of rcdmReference("Patient", "678910")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsRespiratoryRateJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsRespiratoryRate)

        // Uncomment to take a peek at the JSON
        // println(roninObsRespiratoryRateJSON)
        assertNotNull(roninObsRespiratoryRateJSON)
        assertNotNull(roninObsRespiratoryRate.meta)
        assertEquals(
            roninObsRespiratoryRate.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_RESPIRATORY_RATE.value
        )
        assertNotNull(roninObsRespiratoryRate.status)
        assertEquals(1, roninObsRespiratoryRate.category.size)
        assertNotNull(roninObsRespiratoryRate.code)
        assertNotNull(roninObsRespiratoryRate.subject)
        assertNotNull(roninObsRespiratoryRate.subject?.type?.extension)
        assertEquals("vital-signs", roninObsRespiratoryRate.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsRespiratoryRate.category[0].coding[0].system)
        assertNotNull(roninObsRespiratoryRate.status)
        assertNotNull(roninObsRespiratoryRate.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `generates valid roninObservationRespiratoryRate Observation`() {
        val roninObsRespRate = rcdmObservationRespiratoryRate("fake-tenant") {}
        assertNull(roninObsRespRate.id)
        assertNotNull(roninObsRespRate.meta)
        assertEquals(
            roninObsRespRate.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_RESPIRATORY_RATE.value
        )
        assertNull(roninObsRespRate.implicitRules)
        assertNull(roninObsRespRate.language)
        assertNull(roninObsRespRate.text)
        assertEquals(0, roninObsRespRate.contained.size)
        assertEquals(1, roninObsRespRate.extension.size)
        assertEquals(0, roninObsRespRate.modifierExtension.size)
        assertTrue(roninObsRespRate.identifier.size >= 3)
        assertTrue(roninObsRespRate.identifier.any { it.value == "fake-tenant".asFHIR() })
        assertTrue(roninObsRespRate.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsRespRate.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsRespRate.status)
        assertEquals(1, roninObsRespRate.category.size)
        assertNotNull(roninObsRespRate.code)
        assertNotNull(roninObsRespRate.subject)
        assertNotNull(roninObsRespRate.subject?.type?.extension)
        assertEquals("vital-signs", roninObsRespRate.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsRespRate.category[0].coding[0].system)
        assertNotNull(roninObsRespRate.status)
        assertNotNull(roninObsRespRate.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for roninObservationRespiratoryRate`() {
        val roninObsRespRate = rcdmObservationRespiratoryRate("test") {}
        val validation = roninRespRate.validate(roninObsRespRate, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `validation for roninObservationRespiratoryRate with existing identifier`() {
        val roninObsRespRate = rcdmObservationRespiratoryRate("test") {
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
        }
        val validation = roninRespRate.validate(roninObsRespRate, null)
        assertEquals(validation.hasErrors(), false)
        assertNotNull(roninObsRespRate.meta)
        assertNotNull(roninObsRespRate.identifier)
        assertEquals(4, roninObsRespRate.identifier.size)
        assertNotNull(roninObsRespRate.status)
        assertNotNull(roninObsRespRate.code)
        assertNotNull(roninObsRespRate.subject)
    }

    @Test
    fun `validation fails for roninObservationRespiratoryRate with bad code`() {
        val roninObsRespRate = rcdmObservationRespiratoryRate("test") {
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
        val validation = roninRespRate.validate(roninObsRespRate, null)
        assertTrue(validation.hasErrors())

        val issueCodes = validation.issues().map { it.code }.toSet()
        assertEquals(setOf("RONIN_OBS_003"), issueCodes)
    }
}
