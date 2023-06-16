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
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninPulseOximetry
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninPulseOximetryGeneratorTest {
    private lateinit var roninPulseOx: RoninPulseOximetry
    private lateinit var registry: NormalizationRegistryClient
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val flowRateCoding = listOf(
        Coding(system = CodeSystem.LOINC.uri, code = Code("3151-8"))
    )
    private val concentrationCoding = listOf(
        Coding(system = CodeSystem.LOINC.uri, code = Code("3150-0"))
    )

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
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)
            } returns possiblePulseOximetryCodes
            every {
                getRequiredValueSet("Observation.component:FlowRate.code", RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)
            } returns flowRateCoding
            every {
                getRequiredValueSet(
                    "Observation.component:Concentration.code",
                    RoninProfile.OBSERVATION_PULSE_OXIMETRY.value
                )
            } returns concentrationCoding
        }
        roninPulseOx = RoninPulseOximetry(normalizer, localizer, registry)
    }

    @Test
    fun `example use for roninObservationPulseOximetry`() {
        // Create PulseOximetry Obs with attributes you need, provide the tenant(mda), here "fake-tenant"
        val roninObsPulseOximetry = rcdmObservationPulseOximetry("fake-tenant") {
            // if you want to test for a specific status
            status of Code("registered-different")
            // test for a new or different code
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        version of "0.01"
                        code of Code("94499-1")
                        display of "Respiratory viral pathogens DNA and RNA panel - Respiratory specimen Qualitative by NAA with probe detection"
                    }
                )
                text of "Respiratory viral pathogens DNA and RNA panel" // text is kept if provided otherwise only a code.coding is generated
            }
            // test for a specific subject / patient - here you pass 'type' of PATIENT and 'id' of 678910
            subject of rcdmReference("Patient", "678910")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsPulseOximetryJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsPulseOximetry)

        // Uncomment to take a peek at the JSON
        // println(roninObsBloodPressureJSON)
        assertNotNull(roninObsPulseOximetryJSON)
    }

    @Test
    fun `example use for roninObservationPulseOximetry - missing required fields generated`() {
        // Create PulseOximetry Obs with attributes you need, provide the tenant(mda), here "fake-tenant"
        val roninObsPulseOximetry = rcdmObservationPulseOximetry("fake-tenant") {
            // status, code and category required and will be generated
            // test for a specific subject / patient - here you pass 'type' of PATIENT and 'id' of 678910
            subject of rcdmReference("Patient", "678910")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsPulseOximetryJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsPulseOximetry)

        // Uncomment to take a peek at the JSON
        // println(roninObsPulseOximetryJSON)
        assertNotNull(roninObsPulseOximetryJSON)
        assertNotNull(roninObsPulseOximetry.meta)
        assertEquals(
            roninObsPulseOximetry.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_PULSE_OXIMETRY.value
        )
        assertNotNull(roninObsPulseOximetry.status)
        assertEquals(1, roninObsPulseOximetry.category.size)
        assertNotNull(roninObsPulseOximetry.code)
        assertNotNull(roninObsPulseOximetry.subject)
        assertNotNull(roninObsPulseOximetry.subject?.type?.extension)
        assertEquals("vital-signs", roninObsPulseOximetry.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsPulseOximetry.category[0].coding[0].system)
        assertNotNull(roninObsPulseOximetry.status)
        assertNotNull(roninObsPulseOximetry.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `generates valid roninObservationPulseOximetry Observation`() {
        val roninObsPulseOx = rcdmObservationPulseOximetry("fake-tenant") {}
        assertNull(roninObsPulseOx.id)
        assertNotNull(roninObsPulseOx.meta)
        assertEquals(
            roninObsPulseOx.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_PULSE_OXIMETRY.value
        )
        assertNull(roninObsPulseOx.implicitRules)
        assertNull(roninObsPulseOx.language)
        assertNull(roninObsPulseOx.text)
        assertEquals(0, roninObsPulseOx.contained.size)
        assertEquals(1, roninObsPulseOx.extension.size)
        assertEquals(0, roninObsPulseOx.modifierExtension.size)
        assertTrue(roninObsPulseOx.identifier.size >= 3)
        assertTrue(roninObsPulseOx.identifier.any { it.value == "fake-tenant".asFHIR() })
        assertTrue(roninObsPulseOx.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsPulseOx.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsPulseOx.status)
        assertEquals(1, roninObsPulseOx.category.size)
        assertNotNull(roninObsPulseOx.code)
        assertNotNull(roninObsPulseOx.subject)
        assertNotNull(roninObsPulseOx.subject?.type?.extension)
        assertEquals("vital-signs", roninObsPulseOx.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsPulseOx.category[0].coding[0].system)
        assertNotNull(roninObsPulseOx.status)
        assertNotNull(roninObsPulseOx.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for roninObservationPulseOximetry`() {
        val roninPulseOximetry = rcdmObservationPulseOximetry("test") {}
        val validation = roninPulseOx.validate(roninPulseOximetry, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `validation for roninObservationPulseOximetry with existing identifier`() {
        val roninPulseOximetry = rcdmObservationPulseOximetry("test") {
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
        }
        val validation = roninPulseOx.validate(roninPulseOximetry, null)
        assertEquals(validation.hasErrors(), false)
        assertNotNull(roninPulseOximetry.meta)
        assertNotNull(roninPulseOximetry.identifier)
        assertEquals(4, roninPulseOximetry.identifier.size)
        assertNotNull(roninPulseOximetry.status)
        assertNotNull(roninPulseOximetry.code)
        assertNotNull(roninPulseOximetry.subject)
        assertNotNull(roninPulseOximetry.value)
        assertNotNull(roninPulseOximetry.component)
    }

    @Test
    fun `validation fails for roninObservationPulseOximetry with bad code`() {
        val roninPulseOximetry = rcdmObservationPulseOximetry("test") {
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
        val validation = roninPulseOx.validate(roninPulseOximetry, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals(validation.issues()[0].code, "RONIN_NOV_CODING_001")
        assertEquals(validation.issues()[1].code, "RONIN_OBS_003")
    }
}
