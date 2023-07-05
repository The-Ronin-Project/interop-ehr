package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
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
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyTemperature
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninBodyTemperatureGeneratorTest {
    private lateinit var roninBodyTemp: RoninBodyTemperature
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
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_TEMPERATURE.value)
            } returns possibleBodyTemperatureCodes
        }
        roninBodyTemp = RoninBodyTemperature(normalizer, localizer, registry)
    }

    @Test
    fun `example use for roninObservationBodyTemperature`() {
        // Create BodyTemperature Obs with attributes you need, provide the tenant
        val roninObsBodyTemperature = rcdmObservationBodyTemperature("test") {
            // if you want to test for a specific status
            status of Code("status")
            // test for a new or different code
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        code of Code("9848-3")
                        display of "Body temperature special circumstances"
                    }
                )
                text of "Body temperature special circumstances" // text is kept if provided otherwise only a code.coding is generated
            }
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBodyTemperatureJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBodyTemperature)

        // Uncomment to take a peek at the JSON
        // println(roninObsBodyTemperatureJSON)
        assertNotNull(roninObsBodyTemperatureJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationBodyTemperature - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsBodyTemperature = rcdmPatient.rcdmObservationBodyTemperature {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBodyTemperatureJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBodyTemperature)

        // Uncomment to take a peek at the JSON
        // println(roninObsBodyTemperatureJSON)
        assertNotNull(roninObsBodyTemperatureJSON)
        assertNotNull(roninObsBodyTemperature.meta)
        assertEquals(
            roninObsBodyTemperature.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BODY_TEMPERATURE.value
        )
        assertNotNull(roninObsBodyTemperature.status)
        assertEquals(1, roninObsBodyTemperature.category.size)
        assertNotNull(roninObsBodyTemperature.code)
        assertNotNull(roninObsBodyTemperature.subject)
        assertNotNull(roninObsBodyTemperature.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBodyTemperature.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBodyTemperature.category[0].coding[0].system)
        assertNotNull(roninObsBodyTemperature.status)
        assertNotNull(roninObsBodyTemperature.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsBodyTemperature.id)
        val patientFHIRId = roninObsBodyTemperature.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant = roninObsBodyTemperature.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsBodyTemperature.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationBodyTemperature Observation`() {
        val roninObsBodyTemp = rcdmObservationBodyTemperature("test") {}
        assertNotNull(roninObsBodyTemp.id)
        assertNotNull(roninObsBodyTemp.meta)
        assertEquals(
            roninObsBodyTemp.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BODY_TEMPERATURE.value
        )
        assertNull(roninObsBodyTemp.implicitRules)
        assertNull(roninObsBodyTemp.language)
        assertNull(roninObsBodyTemp.text)
        assertEquals(0, roninObsBodyTemp.contained.size)
        assertEquals(1, roninObsBodyTemp.extension.size)
        assertEquals(0, roninObsBodyTemp.modifierExtension.size)
        assertTrue(roninObsBodyTemp.identifier.size >= 3)
        assertTrue(roninObsBodyTemp.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsBodyTemp.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsBodyTemp.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsBodyTemp.status)
        assertEquals(1, roninObsBodyTemp.category.size)
        assertNotNull(roninObsBodyTemp.code)
        assertNotNull(roninObsBodyTemp.subject)
        assertNotNull(roninObsBodyTemp.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBodyTemp.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBodyTemp.category[0].coding[0].system)
        assertNotNull(roninObsBodyTemp.status)
        assertNotNull(roninObsBodyTemp.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for roninObservationBodyTemperature`() {
        val bodyTemp = rcdmObservationBodyTemperature("test") {}
        val validation = roninBodyTemp.validate(bodyTemp, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `validation for roninObservationBodyTemperature with existing identifier`() {
        val bodyTemp = rcdmObservationBodyTemperature("test") {
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
        }
        val validation = roninBodyTemp.validate(bodyTemp, null)
        assertEquals(validation.hasErrors(), false)
        assertNotNull(bodyTemp.meta)
        assertNotNull(bodyTemp.identifier)
        assertEquals(4, bodyTemp.identifier.size)
        assertNotNull(bodyTemp.status)
        assertNotNull(bodyTemp.code)
        assertNotNull(bodyTemp.subject)
    }

    @Test
    fun `validation passed for roninObservationBodyTemperature with code`() {
        val bodyTemp = rcdmObservationBodyTemperature("test") {
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        version of "2.74"
                        code of Code("8333-7")
                        display of "Tympanic membrane temperature"
                    }
                )
            }
        }
        val validation = roninBodyTemp.validate(bodyTemp, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `validation fails for roninObservationBodyTemperature with bad code`() {
        val bodyTemp = rcdmObservationBodyTemperature("test") {
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
        val validation = roninBodyTemp.validate(bodyTemp, null)
        assertTrue(validation.hasErrors())

        val issueCodes = validation.issues().map { it.code }.toSet()
        assertEquals(setOf("RONIN_OBS_003"), issueCodes)
    }
}
